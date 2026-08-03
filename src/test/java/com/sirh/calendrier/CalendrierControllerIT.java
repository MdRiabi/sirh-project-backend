package com.sirh.calendrier;

import com.sirh.AbstractIntegrationTest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.entity.Role;
import com.sirh.demandes.entity.HistoriqueSolde;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.HistoriqueSoldeRepository;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre le CRUD jours_feries + le recrédit automatique de solde
 * (RECREDIT_FERIE) en cas de chevauchement férié/congé validé
 * (cahier-fonctionnel.md Module 6).
 */
class CalendrierControllerIT extends AbstractIntegrationTest {

    @Autowired
    private IndisponibiliteRepository indisponibiliteRepository;
    @Autowired
    private HistoriqueSoldeRepository historiqueSoldeRepository;
    @Autowired
    private EmployeRepository employeRepository;

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

    private ResponseEntity<Map> creerJourFerie(String token, LocalDate date, String libelle, String type) {
        Map<String, Object> body = Map.of("date", date.toString(), "libelle", libelle, "type", type);
        return restTemplate.exchange(baseUrl("/calendrier/jours-feries"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @Test
    void creerFerie_surUnCongeValide_recredit1JourAuSolde() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-CAL-1").nom("Test").prenom("User").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).soldeCongesJours(BigDecimal.valueOf(10)).build());

        LocalDate ferie = LocalDate.now().plusDays(15);
        indisponibiliteRepository.save(Indisponibilite.builder()
                .employeId(employe.getId())
                .typeIndisponibilite("CONGE_PAYE")
                .dateDebut(ferie.minusDays(2))
                .dateFin(ferie.plusDays(2))
                .statut("VALIDE")
                .creeParId(employe.getId())
                .creeParRole("RH")
                .build());

        ResponseEntity<Map> reponse = creerJourFerie(token, ferie, "Aïd Mobile", "FERIE");
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Employe rechargé = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(rechargé.getSoldeCongesJours()).isEqualByComparingTo("11");

        List<HistoriqueSolde> mouvements = historiqueSoldeRepository.findAll().stream()
                .filter(h -> h.getEmployeId().equals(employe.getId())).toList();
        assertThat(mouvements).hasSize(1);
        assertThat(mouvements.get(0).getMotif()).isEqualTo("RECREDIT_FERIE");
        assertThat(mouvements.get(0).getDeltaJours()).isEqualByComparingTo("1");
        assertThat(mouvements.get(0).getSoldeApres()).isEqualByComparingTo("11");
    }

    // Régression /code-review --fix : le recrédit ne doit s'appliquer qu'au congé
    // payé (CONGE_PAYE), pas à n'importe quel type d'indisponibilité validé —
    // un arrêt maladie ne doit jamais créditer un jour de congé payé.
    @Test
    void creerFerie_surUneMaladieValidee_neRecreditePas() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-CAL-3").nom("Test").prenom("User").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).soldeCongesJours(BigDecimal.valueOf(10)).build());

        LocalDate ferie = LocalDate.now().plusDays(17);
        indisponibiliteRepository.save(Indisponibilite.builder()
                .employeId(employe.getId())
                .typeIndisponibilite("MALADIE")
                .dateDebut(ferie.minusDays(1))
                .dateFin(ferie.plusDays(1))
                .statut("VALIDE")
                .creeParId(employe.getId())
                .creeParRole("RH")
                .build());

        ResponseEntity<Map> reponse = creerJourFerie(token, ferie, "Férié sur maladie", "FERIE");
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Employe rechargé = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(rechargé.getSoldeCongesJours()).isEqualByComparingTo("10");
    }

    @Test
    void creerFermetureCollective_surUnCongeValide_neRecreditePas() {
        String token = accessTokenFor(Role.ROLE_RH);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-CAL-2").nom("Test").prenom("User").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).soldeCongesJours(BigDecimal.valueOf(10)).build());

        LocalDate fermeture = LocalDate.now().plusDays(16);
        indisponibiliteRepository.save(Indisponibilite.builder()
                .employeId(employe.getId())
                .typeIndisponibilite("CONGE_PAYE")
                .dateDebut(fermeture.minusDays(1))
                .dateFin(fermeture.plusDays(1))
                .statut("VALIDE")
                .creeParId(employe.getId())
                .creeParRole("RH")
                .build());

        ResponseEntity<Map> reponse = creerJourFerie(token, fermeture, "Fermeture usine", "FERMETURE_COLLECTIVE");
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Employe rechargé = employeRepository.findById(employe.getId()).orElseThrow();
        assertThat(rechargé.getSoldeCongesJours()).isEqualByComparingTo("10");
    }

    @Test
    void importer_avecDateEnDoublon_rollbackTotalAucuneInsertion() {
        String token = accessTokenFor(Role.ROLE_RH);
        LocalDate d1 = LocalDate.now().plusDays(60);
        LocalDate d2 = LocalDate.now().plusDays(61);

        // d1 existe déjà en base -> l'import complet doit échouer sans rien insérer,
        // y compris d2 qui elle est valide.
        creerJourFerie(token, d1, "Existant", "FERIE");

        List<Map<String, Object>> lignes = List.of(
                Map.of("date", d2.toString(), "libelle", "Nouveau", "type", "FERIE"),
                Map.of("date", d1.toString(), "libelle", "Doublon", "type", "FERIE"));
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/calendrier/jours-feries/import"), HttpMethod.POST,
                new HttpEntity<>(lignes, authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<List> liste = restTemplate.exchange(
                baseUrl("/calendrier/jours-feries?dateDebut=" + d2 + "&dateFin=" + d2), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), List.class);
        assertThat(liste.getBody()).isEmpty();
    }

    // Régression /code-review --fix : @Valid ne cascade pas sur les éléments
    // d'un List<T> en @RequestBody — sans la validation manuelle ajoutée dans
    // le contrôleur, une ligne sans libellé aurait échoué en 409 (contrainte
    // NOT NULL) au lieu d'un 400 explicite, incohérent avec le POST unitaire.
    @Test
    void importer_avecLigneInvalide_estRefuseAvec400() {
        String token = accessTokenFor(Role.ROLE_RH);
        LocalDate jour = LocalDate.now().plusDays(90);

        List<Map<String, Object>> lignes = List.of(
                Map.of("date", jour.toString(), "type", "FERIE"));
        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/calendrier/jours-feries/import"), HttpMethod.POST,
                new HttpEntity<>(lignes, authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_parManager_estRefuseAvec403() {
        String token = accessTokenFor(Role.ROLE_MANAGER);
        ResponseEntity<Map> reponse = creerJourFerie(token, LocalDate.now().plusDays(70), "Test", "FERIE");
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void list_retourneLesFeriesDeLaPeriode() {
        String token = accessTokenFor(Role.ROLE_RH);
        LocalDate jour = LocalDate.now().plusDays(80);
        creerJourFerie(token, jour, "Fête du Travail", "FERIE");

        ResponseEntity<List> reponse = restTemplate.exchange(
                baseUrl("/calendrier/jours-feries?dateDebut=" + jour.minusDays(1) + "&dateFin=" + jour.plusDays(1)),
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), List.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).hasSize(1);
    }
}
