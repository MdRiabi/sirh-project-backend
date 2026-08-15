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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 9 (Lot 10) : cycle complet Manager (création + complétion de la
 * grille, blocage 400 si incomplète) -> RH (validation + génération du PDI),
 * et restriction "son équipe" du Manager résolue par département — même
 * convention que OutillageControllerIT.
 */
class EvaluationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DepartementRepository departementRepository;
    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private record FixtureManager(String token, Departement departement) {}

    private Departement creerDepartement() {
        return departementRepository.save(Departement.builder().nom("Dept-" + UUID.randomUUID().toString().substring(0, 8)).build());
    }

    private FixtureManager creerManagerAvecDepartement(Departement departement) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-MGR-" + suffix).nom("Manager").prenom("Test").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).departement(departement).build());
        String username = "it-mgr-" + suffix;
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe).username(username).passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(Role.ROLE_MANAGER).compteActif(true).echecsLoginConsecutifs(0).build());
        return new FixtureManager(login(username), departement);
    }

    private String rhToken() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-RH-" + suffix).nom("RH").prenom("Test").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).build());
        String username = "it-rh-" + suffix;
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe).username(username).passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(Role.ROLE_RH).compteActif(true).echecsLoginConsecutifs(0).build());
        return login(username);
    }

    private Employe creerEmployeDuDepartement(Departement departement) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return employeRepository.save(Employe.builder()
                .matriculeInterne("IT-EMP-" + suffix).nom("Membre").prenom("Equipe").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).departement(departement).build());
    }

    private String login(String username) {
        return restTemplate.postForEntity(baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class)
                .getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> competence(String nom, Object note, Object seuil) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("competence", nom);
        map.put("note", note);
        map.put("seuilExigence", seuil);
        map.put("commentaire", null);
        return map;
    }

    @Test
    void creer_employeHorsDuDepartementDuManager_estRefuseAvec403() {
        FixtureManager manager = creerManagerAvecDepartement(creerDepartement());
        Employe employeAutreDepartement = creerEmployeDuDepartement(creerDepartement());

        Map<String, Object> body = Map.of("employeId", employeAutreDepartement.getId(),
                "competences", List.of(competence("Rigueur", 15, 10)));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void creer_grilleAvecCompetenceNonNotee_estAccepteeCommeBrouillon() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);

        Map<String, Object> body = Map.of("employeId", employe.getId(),
                "competences", List.of(competence("Rigueur", 15, 10), competence("Esprit d'équipe", null, 10)));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<Object>) reponse.getBody().get("competencesManquantes")).containsExactly("Esprit d'équipe");
    }

    @Test
    void mettreAJour_avecCompetenceEncoreNonNotee_estRefuseAvec400EtListeLesManquantes() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        String evaluationReference = creerEvaluation(manager.token(), employe.getId(),
                List.of(competence("Rigueur", 15, 10), competence("Esprit d'équipe", null, 10)));

        Map<String, Object> body = Map.of("competences", List.of(
                competence("Rigueur", 15, 10), competence("Esprit d'équipe", null, 10)));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((List<Object>) reponse.getBody().get("competencesManquantes")).containsExactly("Esprit d'équipe");
    }

    @Test
    void mettreAJour_grilleCompletee_reussitEtLeveLeBlocage() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        String evaluationReference = creerEvaluation(manager.token(), employe.getId(),
                List.of(competence("Rigueur", null, 10)));

        Map<String, Object> body = Map.of("competences", List.of(competence("Rigueur", 15, 10)));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) reponse.getBody().get("competencesManquantes")).isEmpty();
    }

    @Test
    void valider_competenceSousLeSeuil_genereUnPdiEtEstReserveAuRh() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        String evaluationReference = creerEvaluation(manager.token(), employe.getId(),
                List.of(competence("Rigueur", 6, 10)));

        ResponseEntity<Map> refusManager = restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference + "/valider"),
                HttpMethod.PUT, new HttpEntity<>(authHeaders(manager.token())), Map.class);
        assertThat(refusManager.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference + "/valider"),
                HttpMethod.PUT, new HttpEntity<>(authHeaders(rhToken())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().get("statut")).isEqualTo("VALIDEE_RH");
        List<?> pdi = (List<?>) reponse.getBody().get("plansDeveloppement");
        assertThat(pdi).hasSize(1);
        assertThat(((Map<?, ?>) pdi.get(0)).get("competence")).isEqualTo("Rigueur");
    }

    @Test
    void valider_deuxFois_estRefuseeAvec409() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        String evaluationReference = creerEvaluation(manager.token(), employe.getId(), List.of(competence("Rigueur", 15, 10)));
        String rh = rhToken();
        restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(authHeaders(rh)), Map.class);

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations/" + evaluationReference + "/valider"),
                HttpMethod.PUT, new HttpEntity<>(authHeaders(rh)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void parEmploye_managerDUnAutreDepartement_estRefuseAvec403() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        creerEvaluation(manager.token(), employe.getId(), List.of(competence("Rigueur", 15, 10)));

        FixtureManager autreManager = creerManagerAvecDepartement(creerDepartement());
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations/" + employe.getId()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(autreManager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void search_parStatutEnCours_estAccessibleAuRhEtListeLaGrille() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe employe = creerEmployeDuDepartement(departement);
        creerEvaluation(manager.token(), employe.getId(), List.of(competence("Rigueur", 15, 10)));

        ResponseEntity<List> reponse = restTemplate.exchange(baseUrl("/evaluations?statut=EN_COURS"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken())), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isNotEmpty();
    }

    @SuppressWarnings("unchecked")
    private String creerEvaluation(String managerToken, UUID employeId, List<Map<String, Object>> competences) {
        Map<String, Object> body = Map.of("employeId", employeId, "competences", competences);
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/evaluations"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(managerToken)), Map.class);
        return (String) reponse.getBody().get("reference");
    }
}
