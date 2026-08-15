package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.JourFerie;
import com.sirh.model.enums.TypeJourFerie;
import com.sirh.model.repository.JourFerieRepository;
import com.sirh.model.repository.HistoriqueSoldeRepository;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre le workflow d'indisponibilité (Module 8) : déduction du solde en
 * jours ouvrés (week-ends + jours fériés exclus, interfacé avec le Lot 4),
 * forçage RH sur solde insuffisant, refus, mise à jour du planning à la
 * validation (Lot 5, décisions d'architecture explicites de l'utilisateur).
 */
class IndisponibiliteControllerIT extends AbstractIntegrationTest {

    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private HistoriqueSoldeRepository historiqueSoldeRepository;
    @Autowired
    private JourFerieRepository jourFerieRepository;

    private String accessTokenFor(Role role) {
        return login(createFixtureUser(role));
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

    private Employe employeAvecSolde(BigDecimal solde) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return employeRepository.save(Employe.builder()
                .matriculeInterne("IT-INDISPO-" + suffix).nom("Test").prenom("User").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).soldeCongesJours(solde).build());
    }

    // Prochain lundi strictement après `apres`, pour bâtir des périodes
    // Lun-Ven prévisibles (5 jours ouvrés) indépendamment du jour d'exécution.
    private LocalDate prochainLundiApres(LocalDate apres) {
        LocalDate jour = apres.plusDays(1);
        while (jour.getDayOfWeek() != DayOfWeek.MONDAY) {
            jour = jour.plusDays(1);
        }
        return jour;
    }

    private ResponseEntity<Map> creer(String token, Map<String, Object> body) {
        return restTemplate.exchange(baseUrl("/indisponibilites"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @Test
    void valider_congePaye_deduitLesJoursOuvresDuLundiAuVendredi() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(10));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(60));
        LocalDate vendredi = lundi.plusDays(4);

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", vendredi.toString());
        ResponseEntity<Map> creation = creer(rhToken, body);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creation.getBody().get("statut")).isEqualTo("VALIDE");

        Employe recharge = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(recharge.getSoldeCongesJours()).isEqualByComparingTo("5");
    }

    @Test
    void valider_congePaye_excludWeekendEtJourFerieDuDecompte() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(10));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(70));
        LocalDate dimancheSuivant = lundi.plusDays(6); // lundi..dimanche = 7 jours calendaires

        LocalDate mercredi = lundi.plusDays(2);
        jourFerieRepository.save(JourFerie.builder()
                .date(mercredi).libelle("Férié test indispo").type(TypeJourFerie.FERIE).national(true).build());

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", dimancheSuivant.toString());
        creer(rhToken, body);

        // Lun,Mar,Jeu,Ven ouvrés (Mer férié exclu, Sam/Dim exclus) = 4 jours.
        Employe recharge = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(recharge.getSoldeCongesJours()).isEqualByComparingTo("6");
    }

    @Test
    void valider_soldeInsuffisantSansForcage_estRefuseAvec409() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(2));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(80));

        String managerToken = accessTokenFor(Role.ROLE_MANAGER);
        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(4).toString(),
                "avisManagerStatut", "FAVORABLE");
        ResponseEntity<Map> creation = creer(managerToken, body);
        String reference = (String) creation.getBody().get("reference");

        ResponseEntity<Map> validation = restTemplate.exchange(
                baseUrl("/indisponibilites/" + reference + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(null, authHeaders(rhToken)), Map.class);

        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(validation.getBody().get("message").toString()).contains("Solde insuffisant");

        Employe inchange = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(inchange.getSoldeCongesJours()).isEqualByComparingTo("2");
    }

    // Workflow normal (Manager saisit -> RH force à la validation).
    @Test
    void valider_soldeInsuffisantAvecForcage_deduitEnNegatifEtTraceForcageRh() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        String managerToken = accessTokenFor(Role.ROLE_MANAGER);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(2));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(90));

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(4).toString(),
                "avisManagerStatut", "FAVORABLE");
        ResponseEntity<Map> creation = creer(managerToken, body);
        assertThat(creation.getBody().get("statut")).isEqualTo("EN_ATTENTE_RH");
        String reference = (String) creation.getBody().get("reference");

        ResponseEntity<Map> validation = restTemplate.exchange(
                baseUrl("/indisponibilites/" + reference + "/valider"), HttpMethod.PUT,
                new HttpEntity<>(Map.of("forcer", true), authHeaders(rhToken)), Map.class);

        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.OK);
        Employe recharge = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(recharge.getSoldeCongesJours()).isEqualByComparingTo("-3");

        boolean forcageTrace = historiqueSoldeRepository.findAll().stream()
                .anyMatch(h -> h.getEmployeId().equals(employe.getId()) && "FORCAGE_RH".equals(h.getMotif()));
        assertThat(forcageTrace).isTrue();
    }

    // Workflow d'urgence RH (statut VALIDE dès la création) : la déduction —
    // et donc le besoin de forçage — s'applique immédiatement à la création,
    // pas seulement via un valider() séparé (bug trouvé en vérification
    // manuelle docker compose + curl — corrigé dans IndisponibiliteService).
    @Test
    void creerUrgenceRh_soldeInsuffisantAvecForcage_deduitEnNegatifDesLaCreation() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(2));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(95));

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(4).toString(),
                "forcer", true);
        ResponseEntity<Map> creation = creer(rhToken, body);

        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creation.getBody().get("statut")).isEqualTo("VALIDE");
        Employe recharge = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(recharge.getSoldeCongesJours()).isEqualByComparingTo("-3");

        boolean forcageTrace = historiqueSoldeRepository.findAll().stream()
                .anyMatch(h -> h.getEmployeId().equals(employe.getId()) && "FORCAGE_RH".equals(h.getMotif()));
        assertThat(forcageTrace).isTrue();
    }

    // Sans forcer=true, l'urgence RH doit être bloquée dès la création (409),
    // exactement comme le workflow normal à la validation — pas de bypass
    // silencieux de la règle de solde insuffisant sur ce chemin.
    @Test
    void creerUrgenceRh_soldeInsuffisantSansForcage_estRefuseAvec409EtRienNEstCree() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(2));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(96));

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(4).toString());
        ResponseEntity<Map> creation = creer(rhToken, body);

        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Employe inchange = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(inchange.getSoldeCongesJours()).isEqualByComparingTo("2");
    }

    @Test
    void valider_congeSansSolde_neDeduitPasLeSolde() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(10));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(100));

        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_SANS_SOLDE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(2).toString());
        creer(rhToken, body);

        Employe recharge = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(recharge.getSoldeCongesJours()).isEqualByComparingTo("10");
    }

    @Test
    void refuser_parRh_changeStatutSansDeduction() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(10));
        LocalDate lundi = prochainLundiApres(LocalDate.now().plusDays(110));

        String managerToken = accessTokenFor(Role.ROLE_MANAGER);
        Map<String, Object> body = Map.of(
                "employeId", employe.getId(), "typeIndisponibilite", "CONGE_PAYE",
                "dateDebut", lundi.toString(), "dateFin", lundi.plusDays(1).toString(),
                "avisManagerStatut", "DEFAVORABLE");
        ResponseEntity<Map> creation = creer(managerToken, body);
        String reference = (String) creation.getBody().get("reference");

        ResponseEntity<Map> refus = restTemplate.exchange(
                baseUrl("/indisponibilites/" + reference + "/refuser"), HttpMethod.PUT,
                new HttpEntity<>(null, authHeaders(rhToken)), Map.class);

        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refus.getBody().get("statut")).isEqualTo("REFUSE");
        Employe inchange = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(inchange.getSoldeCongesJours()).isEqualByComparingTo("10");
    }

    @Test
    void getSolde_retourneLeSoldeCourant() {
        String rhToken = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeAvecSolde(BigDecimal.valueOf(7.5));

        ResponseEntity<Map> reponse = restTemplate.exchange(
                baseUrl("/indisponibilites/solde/" + employe.getId()), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new BigDecimal(reponse.getBody().get("soldeCongesJours").toString())).isEqualByComparingTo("7.5");
    }

    @Test
    void list_parManager_neVoitQueSonPropreDepartement() {
        String managerToken = accessTokenFor(Role.ROLE_MANAGER);
        Employe employeAutreDepartement = employeAvecSolde(BigDecimal.TEN);
        String rhToken = accessTokenFor(Role.ROLE_RH);
        creer(rhToken, Map.of(
                "employeId", employeAutreDepartement.getId(), "typeIndisponibilite", "MALADIE",
                "dateDebut", LocalDate.now().toString(), "dateFin", LocalDate.now().plusDays(1).toString()));

        ResponseEntity<List> reponse = restTemplate.exchange(baseUrl("/indisponibilites"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEmpty();
    }
}
