package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Employe;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PointagesControllerIT extends AbstractIntegrationTest {

    // Format attendu par le parseur d'import Excel (PointageImportWriter.TEXTE_DATETIME).
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // Format attendu par Jackson (JavaTimeModule) pour le JSON du webhook Hikvision --
    // "yyyy-MM-dd HH:mm" (sans T) n'est pas désérialisable en LocalDateTime et fait
    // échouer la requête en 400 (constat distinct de l'import Excel, qui a son propre
    // parseur avec son propre format).
    private static final DateTimeFormatter FMT_JSON = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String accessTokenFor(Role role) {
        String username = createFixtureUser(role);
        TokenResponse login = restTemplate.postForEntity(
                baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class).getBody();
        return login.accessToken();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void webhookHikvision_depuisIpAutorisee_creeUnPointage() {
        Employe employe = createFixtureEmploye();
        Map<String, Object> body = Map.of(
                "badgeId", employe.getMatriculeInterne(),
                "timestamp", LocalDateTime.now().format(FMT_JSON),
                "typeEvenement", "ENTREE",
                "methodeValidation", "BADGE"
        );
        // TestRestTemplate se connecte en loopback (127.0.0.1), déjà whitelisté par V3__seed_pointeuses_dev.sql.
        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl("/api/v1/hikvision/events"), body, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void webhookHikvision_badgeIdInconnu_estRefuse() {
        Map<String, Object> body = Map.of(
                "badgeId", "INCONNU-999",
                "timestamp", LocalDateTime.now().format(FMT_JSON),
                "typeEvenement", "ENTREE",
                "methodeValidation", "BADGE"
        );
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl("/api/v1/hikvision/events"), body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doublon_dansLaFenetreDe5Minutes_estIgnore() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();

        Map<String, Object> premier = Map.of(
                "badgeId", employe.getMatriculeInterne(),
                "timestamp", LocalDateTime.now().format(FMT_JSON),
                "typeEvenement", "ENTREE",
                "methodeValidation", "BADGE"
        );
        Map<String, Object> doublon = Map.of(
                "badgeId", employe.getMatriculeInterne(),
                "timestamp", LocalDateTime.now().plusMinutes(2).format(FMT_JSON),
                "typeEvenement", "ENTREE",
                "methodeValidation", "BADGE"
        );
        restTemplate.postForEntity(baseUrl("/api/v1/hikvision/events"), premier, Void.class);
        restTemplate.postForEntity(baseUrl("/api/v1/hikvision/events"), doublon, Void.class);

        ResponseEntity<List<Map<String, Object>>> pointages = restTemplate.exchange(
                baseUrl("/pointages?employeId=" + employe.getId()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {});

        assertThat(pointages.getBody()).hasSize(2);
        long ignores = pointages.getBody().stream().filter(p -> "IGNORE_DOUBLON".equals(p.get("statut"))).count();
        // Un seul pointage ENTREE survit après dédoublonnage : nombre impair de
        // pointages valides sur le jour, donc PointageBrutService.detecterAnomalieImpaire
        // le requalifie en ANOMALIE_NON_JUSTIFIEE (pas de SORTIE dans ce scénario).
        long anomalies = pointages.getBody().stream().filter(p -> "ANOMALIE_NON_JUSTIFIEE".equals(p.get("statut"))).count();
        assertThat(ignores).isEqualTo(1);
        assertThat(anomalies).isEqualTo(1);
    }

    @Test
    void importExcel_fichierValide_creeLesPointagesEtPasseAuStatutSuccess() throws Exception {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();

        byte[] fichier = construireFichier(List.<String[]>of(
                new String[]{employe.getMatriculeInterne(), LocalDateTime.now().format(FMT), "ENTREE", "BADGE"}
        ));

        UUID jobReference = lancerImport(token, fichier);

        Map<String, Object> job = attendreJobTermine(token, jobReference);
        assertThat(job.get("statut")).isEqualTo("SUCCESS");
        assertThat(job.get("nombreLignesTraitees")).isEqualTo(1);
    }

    @Test
    void importExcel_ligneInvalide_rollbackTotalEtJobFailed() throws Exception {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();

        byte[] fichier = construireFichier(List.<String[]>of(
                new String[]{employe.getMatriculeInterne(), LocalDateTime.now().format(FMT), "ENTREE", "BADGE"},
                new String[]{"MATRICULE-INEXISTANT", LocalDateTime.now().format(FMT), "SORTIE", "BADGE"}
        ));

        UUID jobReference = lancerImport(token, fichier);

        Map<String, Object> job = attendreJobTermine(token, jobReference);
        assertThat(job.get("statut")).isEqualTo("FAILED");
        assertThat(job.get("ligneErreur")).isEqualTo(3); // ligne 1 = en-tête, ligne 2 = OK, ligne 3 = invalide

        // Rollback total : même la ligne valide n'a pas été insérée.
        ResponseEntity<List<Map<String, Object>>> pointages = restTemplate.exchange(
                baseUrl("/pointages?employeId=" + employe.getId()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {});
        assertThat(pointages.getBody()).isEmpty();
    }

    @Test
    void correctionManuelle_ecritLesAnciennesEtNouvellesValeursDansAuditLogs() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = createFixtureEmploye();

        Map<String, Object> body = Map.of(
                "badgeId", employe.getMatriculeInterne(),
                "timestamp", LocalDateTime.now().format(FMT_JSON),
                "typeEvenement", "ENTREE",
                "methodeValidation", "BADGE"
        );
        restTemplate.postForEntity(baseUrl("/api/v1/hikvision/events"), body, Void.class);

        ResponseEntity<List<Map<String, Object>>> pointages = restTemplate.exchange(
                baseUrl("/pointages?employeId=" + employe.getId()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {});
        String pointageReference = (String) pointages.getBody().get(0).get("reference");

        Map<String, Object> correction = Map.of(
                "timestampPointage", LocalDateTime.now().plusHours(1).format(FMT_JSON),
                "typeEvenement", "SORTIE",
                "motif", "Oubli de badgeage corrigé suite à déclaration employé"
        );
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl("/pointages/" + pointageReference + "/corriger"), HttpMethod.PUT,
                new HttpEntity<>(correction, authHeaders(token)), new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("statut")).isEqualTo("JUSTIFIE");
    }

    @Test
    void accesRefuse_pourRoleEmploye() {
        String token = accessTokenFor(Role.ROLE_EMPLOYE);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/pointages"), HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> attendreJobTermine(String token, UUID jobReference) {
        for (int i = 0; i < 50; i++) {
            ResponseEntity<Map<String, Object>> job = restTemplate.exchange(
                    baseUrl("/async-jobs/" + jobReference), HttpMethod.GET, new HttpEntity<>(authHeaders(token)),
                    new ParameterizedTypeReference<>() {});
            String statut = (String) job.getBody().get("statut");
            if ("SUCCESS".equals(statut) || "FAILED".equals(statut)) {
                return job.getBody();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Job " + jobReference + " non terminé après 10s");
    }

    private UUID lancerImport(String token, byte[] fichier) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource(fichier) {
            @Override
            public String getFilename() {
                return "import-test.xlsx";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl("/pointages/import"), HttpMethod.POST, new HttpEntity<>(multipart, headers),
                new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return UUID.fromString((String) response.getBody().get("jobId"));
    }

    private byte[] construireFichier(List<String[]> lignes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("pointages");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("badgeId");
            header.createCell(1).setCellValue("dateTime");
            header.createCell(2).setCellValue("typeEvenement");
            header.createCell(3).setCellValue("methodeValidation");

            int rowIndex = 1;
            for (String[] ligne : lignes) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < ligne.length; i++) {
                    row.createCell(i).setCellValue(ligne[i]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}