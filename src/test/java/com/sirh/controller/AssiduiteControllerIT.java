package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.DepartementRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.entity.PointageBrut;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;
import com.sirh.model.repository.PointageBrutRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre le Lot 6 (Module 3) : préconditions de clôture (taux d'anomalies
 * justifiées, aucune demande EN_ATTENTE_RH sur le mois), verrouillage en
 * écriture d'un mois clôturé sur pointages_bruts/autorisations, et le
 * workflow de déverrouillage exceptionnel de bout en bout.
 */
class AssiduiteControllerIT extends AbstractIntegrationTest {

    @Autowired
    private PointageBrutRepository pointageBrutRepository;
    @Autowired
    private DepartementRepository departementRepository;
    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private record FixtureManager(String token, Long departementId) {}

    // Manager réellement rattaché à un département (contrairement à
    // createFixtureUser(ROLE_MANAGER), qui laisse departement_id=null) —
    // nécessaire pour vérifier le filtre departementId sur
    // GET /assiduite/demandes-deverrouillage.
    private FixtureManager creerManagerAvecDepartement() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Departement departement = departementRepository.save(Departement.builder().nom("Dept-" + suffix).build());
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-MGR-" + suffix).nom("Manager").prenom("Test").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).departement(departement).build());
        String username = "it-mgr-" + suffix;
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe).username(username).passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(Role.ROLE_MANAGER).compteActif(true).echecsLoginConsecutifs(0).build());
        return new FixtureManager(login(username), departement.getId());
    }

    private String login(String username) {
        return restTemplate.postForEntity(baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class)
                .getBody().accessToken();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<Map> cloturer(String token, YearMonth mois) {
        return restTemplate.exchange(baseUrl("/assiduite/cloturer-paie"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", mois.toString()), authHeaders(token)), Map.class);
    }

    private PointageBrut anomalie(java.util.UUID employeId, LocalDateTime timestamp) {
        return pointageBrutRepository.save(PointageBrut.builder()
                .employeId(employeId).timestampPointage(timestamp).typeEvenement(TypeEvenement.ENTREE)
                .methodeValidation(MethodeValidation.BADGE).sourceImport(SourceImport.HIKVISION_PUSH)
                .statut(PointageStatut.ANOMALIE_NON_JUSTIFIEE).build());
    }

    @Test
    void cloturerPaie_moisSansAnomalieEtSansDemandeEnAttente_reussit() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(1);

        ResponseEntity<Map> reponse = cloturer(rhToken, mois);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().get("statut")).isEqualTo("CLOTURE");
    }

    @Test
    void cloturerPaie_dejaCloturee_estRefuseeAvec409() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(2);
        cloturer(rhToken, mois);

        ResponseEntity<Map> reponse = cloturer(rhToken, mois);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("clôturé");
    }

    @Test
    void cloturerPaie_tauxAnomaliesJustifieesSousLeSeuil_estRefuseeAvec409() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        Employe employe = createFixtureEmploye();
        YearMonth mois = YearMonth.now().minusMonths(3);
        anomalie(employe.getId(), mois.atDay(5).atTime(8, 0)); // 0% justifiée < 98% requis

        ResponseEntity<Map> reponse = cloturer(rhToken, mois);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("anomalies justifiées");
    }

    @Test
    void cloturerPaie_demandeEnAttenteRhSurLeMois_estRefuseeAvec409() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        LocalDate aujourdHui = LocalDate.now();

        Map<String, Object> demande = Map.of(
                "employeId", employe.getId(), "type", "RETARD_JUSTIFIE",
                "dateAutorisation", aujourdHui.toString(), "heureDebut", "08:00:00",
                "dureeMinutes", 30, "avisManagerStatut", "FAVORABLE");
        ResponseEntity<Map> creation = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(demande, authHeaders(managerToken)), Map.class);
        assertThat(creation.getBody().get("statut")).isEqualTo("EN_ATTENTE_RH");

        ResponseEntity<Map> reponse = cloturer(rhToken, YearMonth.from(aujourdHui));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("EN_ATTENTE_RH");
    }

    @Test
    void corrigerPointage_surMoisCloture_estRefuseeAvec409() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        Employe employe = createFixtureEmploye();
        YearMonth mois = YearMonth.now().minusMonths(4);
        PointageBrut pointage = pointageBrutRepository.save(PointageBrut.builder()
                .employeId(employe.getId()).timestampPointage(mois.atDay(10).atTime(8, 0)).typeEvenement(TypeEvenement.ENTREE)
                .methodeValidation(MethodeValidation.BADGE).sourceImport(SourceImport.HIKVISION_PUSH)
                .statut(PointageStatut.OK).build());
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> correction = Map.of(
                "timestampPointage", mois.atDay(10).atTime(8, 15).toString(),
                "typeEvenement", "ENTREE", "motif", "Correction test");
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/pointages/" + pointage.getReference() + "/corriger"), HttpMethod.PUT,
                new HttpEntity<>(correction, authHeaders(rhToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("clôturé");
    }

    @Test
    void creerAutorisation_parRhEnUrgenceSurMoisCloture_estRefuseeAvec409() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        Employe employe = createFixtureEmploye();
        YearMonth mois = YearMonth.now().minusMonths(5);
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> demande = Map.of(
                "employeId", employe.getId(), "type", "RETARD_JUSTIFIE",
                "dateAutorisation", mois.atDay(3).toString(), "heureDebut", "08:00:00", "dureeMinutes", 30);
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(demande, authHeaders(rhToken)), Map.class);

        // Même le workflow d'urgence RH (statut VALIDE dès la création, aucun
        // délai de 72h) reste bloqué par le verrou mensuel.
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message").toString()).contains("clôturé");
    }

    @Test
    void workflowDeverrouillageExceptionnel_deBoutEnBout_reouvreLeMoisApresApprobationRh() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        String managerToken = login(createFixtureUser(Role.ROLE_MANAGER));
        Employe employe = createFixtureEmploye();
        YearMonth mois = YearMonth.now().minusMonths(6);
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        // Une demande de déverrouillage sur un mois non clôturé est refusée (400).
        ResponseEntity<Map> demandeMoisOuvert = restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", YearMonth.now().toString(), "motif", "test"), authHeaders(managerToken)), Map.class);
        assertThat(demandeMoisOuvert.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> demande = restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", mois.toString(), "motif", "Oubli de badgeage à régulariser"), authHeaders(managerToken)), Map.class);
        assertThat(demande.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(demande.getBody().get("statut")).isEqualTo("EN_ATTENTE");
        String demandeReference = (String) demande.getBody().get("reference");

        Map<String, Object> autorisationBody = Map.of(
                "employeId", employe.getId(), "type", "RETARD_JUSTIFIE",
                "dateAutorisation", mois.atDay(3).toString(), "heureDebut", "08:00:00", "dureeMinutes", 30);

        // Tant que la demande n'est pas approuvée, le mois reste verrouillé.
        ResponseEntity<Map> ecritureAvantApprobation = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(autorisationBody, authHeaders(rhToken)), Map.class);
        assertThat(ecritureAvantApprobation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> approbation = restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage/" + demandeReference), HttpMethod.PUT,
                new HttpEntity<>(Map.of("statut", "APPROUVEE"), authHeaders(rhToken)), Map.class);
        assertThat(approbation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approbation.getBody().get("statut")).isEqualTo("APPROUVEE");

        ResponseEntity<Map> ecritureApresApprobation = restTemplate.exchange(baseUrl("/autorisations"), HttpMethod.POST,
                new HttpEntity<>(autorisationBody, authHeaders(rhToken)), Map.class);
        assertThat(ecritureApresApprobation.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Retraiter une demande déjà traitée est refusé (409).
        ResponseEntity<Map> retraitement = restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage/" + demandeReference), HttpMethod.PUT,
                new HttpEntity<>(Map.of("statut", "REFUSEE"), authHeaders(rhToken)), Map.class);
        assertThat(retraitement.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void calculMensuel_repondAvecLaSegmentationParEmploye() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        Employe employe = createFixtureEmploye();
        YearMonth mois = YearMonth.now().minusMonths(7);
        pointageBrutRepository.save(PointageBrut.builder()
                .employeId(employe.getId()).timestampPointage(mois.atDay(6).atTime(8, 0)).typeEvenement(TypeEvenement.ENTREE)
                .methodeValidation(MethodeValidation.BADGE).sourceImport(SourceImport.HIKVISION_PUSH).statut(PointageStatut.OK).build());
        pointageBrutRepository.save(PointageBrut.builder()
                .employeId(employe.getId()).timestampPointage(mois.atDay(6).atTime(16, 0)).typeEvenement(TypeEvenement.SORTIE)
                .methodeValidation(MethodeValidation.BADGE).sourceImport(SourceImport.HIKVISION_PUSH).statut(PointageStatut.OK).build());

        ResponseEntity<List> reponse = restTemplate.exchange(
                baseUrl("/assiduite/calcul-mensuel?mois=" + mois), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).anySatisfy(ligne -> {
            Map<?, ?> dto = (Map<?, ?>) ligne;
            if (employe.getId().toString().equals(dto.get("employeId"))) {
                assertThat(dto.get("heuresNormales")).isEqualTo(8.0);
            }
        });
    }

    @Test
    void statutCloture_moisJamaisCloture_renvoieOuvertParDefautSansLigneEnBase() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(8);

        ResponseEntity<Map> reponse = restTemplate.exchange(
                baseUrl("/assiduite/cloture-paie?mois=" + mois), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().get("statut")).isEqualTo("OUVERT");
        assertThat(reponse.getBody().get("reference")).isNull();
    }

    @Test
    void statutCloture_moisCloture_refleteLeStatutSansRelancerLaCloture() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(9);
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> reponse = restTemplate.exchange(
                baseUrl("/assiduite/cloture-paie?mois=" + mois), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().get("statut")).isEqualTo("CLOTURE");
    }

    @Test
    void demandesDeverrouillage_filtreesParDepartement_neRemonteQueCellesDuDemandeurDuDepartement() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(10);
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        FixtureManager managerA = creerManagerAvecDepartement();
        FixtureManager managerB = creerManagerAvecDepartement();
        restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", mois.toString(), "motif", "Demande département A"), authHeaders(managerA.token())), Map.class);
        restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", mois.toString(), "motif", "Demande département B"), authHeaders(managerB.token())), Map.class);

        ResponseEntity<List> reponse = restTemplate.exchange(
                baseUrl("/assiduite/demandes-deverrouillage?mois=" + mois + "&departementId=" + managerA.departementId()),
                HttpMethod.GET, new HttpEntity<>(authHeaders(rhToken)), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).hasSize(1);
        Map<?, ?> demande = (Map<?, ?>) reponse.getBody().get(0);
        assertThat(demande.get("motif")).isEqualTo("Demande département A");
        assertThat(demande.get("departementId")).isEqualTo(managerA.departementId().intValue());
    }

    // Le Manager a désormais accès en lecture à ce endpoint (scope forcé à son
    // propre département) pour savoir si une demande EN_ATTENTE existe déjà
    // sur le mois affiché et éviter une double soumission côté frontend.
    @Test
    void demandesDeverrouillage_parManager_neVoitQueSonPropreDepartement() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));
        YearMonth mois = YearMonth.now().minusMonths(11);
        assertThat(cloturer(rhToken, mois).getStatusCode()).isEqualTo(HttpStatus.OK);

        FixtureManager managerA = creerManagerAvecDepartement();
        FixtureManager managerB = creerManagerAvecDepartement();
        restTemplate.exchange(baseUrl("/assiduite/demandes-deverrouillage"), HttpMethod.POST,
                new HttpEntity<>(Map.of("mois", mois.toString(), "motif", "Demande de B"), authHeaders(managerB.token())), Map.class);

        ResponseEntity<List> vuParA = restTemplate.exchange(
                baseUrl("/assiduite/demandes-deverrouillage?mois=" + mois + "&statut=EN_ATTENTE"),
                HttpMethod.GET, new HttpEntity<>(authHeaders(managerA.token())), List.class);
        assertThat(vuParA.getBody()).isEmpty();

        ResponseEntity<List> vuParB = restTemplate.exchange(
                baseUrl("/assiduite/demandes-deverrouillage?mois=" + mois + "&statut=EN_ATTENTE"),
                HttpMethod.GET, new HttpEntity<>(authHeaders(managerB.token())), List.class);
        assertThat(vuParB.getBody()).hasSize(1);
    }
}
