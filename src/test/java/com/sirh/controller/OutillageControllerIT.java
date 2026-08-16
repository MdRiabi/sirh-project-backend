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
 * Couvre le workflow rattachement/restitution de bout en bout (Module 5,
 * Lot 7) : gestion exclusive au Manager du département de l'outil, transitions
 * check-out/check-in invalides refusées en 409, montant d'imputation calculé
 * et persisté sur une perte déclarée — même convention que AssiduiteControllerIT.
 */
class OutillageControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DepartementRepository departementRepository;
    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private record FixtureManager(String token, Long departementId) {}

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
        return new FixtureManager(login(username), departement.getId());
    }

    private Employe creerEmployeDuDepartement(Departement departement) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return employeRepository.save(Employe.builder()
                .matriculeInterne("IT-EMP-" + suffix).nom("Membre").prenom("Equipe").typeContrat("CDI")
                .dateEmbauche(LocalDate.now()).actif(true).departement(departement).build());
    }

    private String login(String username) {
        return login(username, FIXTURE_PASSWORD).accessToken();
    }

    private Map<?, ?> dataDe(ResponseEntity<Map> reponse) {
        return (Map<?, ?>) reponse.getBody().get("data");
    }

    private String referenceDe(ResponseEntity<Map> reponse) {
        return (String) dataDe(reponse).get("reference");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<Map> creerOutil(String token) {
        Map<String, Object> body = Map.of(
                "codeBarres", "BC-" + UUID.randomUUID().toString().substring(0, 8),
                "nomOutil", "Perceuse", "valeurAchat", 1000, "dateAchat", LocalDate.now().toString());
        return restTemplate.exchange(baseUrl("/outillages"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @Test
    void creer_parManager_creeLOutilScopeASonDepartementAvecEtatDisponible() {
        FixtureManager manager = creerManagerAvecDepartement(creerDepartement());

        ResponseEntity<Map> reponse = creerOutil(manager.token());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(dataDe(reponse).get("etat")).isEqualTo("BON_ETAT");
        assertThat(dataDe(reponse).get("departementId")).isEqualTo(manager.departementId().intValue());
    }

    @Test
    void creer_parRh_estRefuseAvec403_gestionExclusiveAuManager() {
        String rhToken = login(createFixtureUser(Role.ROLE_RH));

        ResponseEntity<Map> reponse = creerOutil(rhToken);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void checkOutPuisCheckIn_workflowNominal_attribueSePuisSeRestitueEnBonEtat() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe membreEquipe = creerEmployeDuDepartement(departement);
        String outilReference = referenceDe(creerOutil(manager.token()));

        ResponseEntity<Map> checkOut = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-out"), HttpMethod.POST,
                new HttpEntity<>(Map.of("employeId", membreEquipe.getId().toString()), authHeaders(manager.token())), Map.class);
        assertThat(checkOut.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(dataDe(checkOut).get("typeMouvement")).isEqualTo("CHECK_OUT");

        ResponseEntity<Map> doubleCheckOut = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-out"), HttpMethod.POST,
                new HttpEntity<>(Map.of("employeId", membreEquipe.getId().toString()), authHeaders(manager.token())), Map.class);
        assertThat(doubleCheckOut.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> checkIn = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-in"), HttpMethod.POST,
                new HttpEntity<>(Map.of("etat", "BON"), authHeaders(manager.token())), Map.class);
        assertThat(checkIn.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // BigDecimal.ZERO se sérialise en JSON sans décimale ("0"), désérialisé en
        // Integer et non Double par Jackson dans un Map brut -- comparaison via
        // Number.doubleValue() pour rester indépendant de ce détail de sérialisation.
        assertThat(((Number) dataDe(checkIn).get("montantImputation")).doubleValue()).isEqualTo(0.0);
    }

    @Test
    void checkIn_sansAttributionPrealable_estRefuseAvec409() {
        FixtureManager manager = creerManagerAvecDepartement(creerDepartement());
        String outilReference = referenceDe(creerOutil(manager.token()));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-in"), HttpMethod.POST,
                new HttpEntity<>(Map.of("etat", "BON"), authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void checkOut_employeHorsDuDepartementDeLOutil_estRefuseAvec403() {
        Departement departementOutil = creerDepartement();
        Departement autreDepartement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departementOutil);
        Employe employeExterne = creerEmployeDuDepartement(autreDepartement);
        String outilReference = referenceDe(creerOutil(manager.token()));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-out"), HttpMethod.POST,
                new HttpEntity<>(Map.of("employeId", employeExterne.getId().toString()), authHeaders(manager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void checkOut_parManagerDUnAutreDepartement_estRefuseAvec403() {
        FixtureManager proprietaire = creerManagerAvecDepartement(creerDepartement());
        FixtureManager autreManager = creerManagerAvecDepartement(creerDepartement());
        String outilReference = referenceDe(creerOutil(proprietaire.token()));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-out"), HttpMethod.POST,
                new HttpEntity<>(Map.of("employeId", UUID.randomUUID().toString()), authHeaders(autreManager.token())), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void checkIn_perteDeclaree_calculeEtPersisteLeMontantImputationVisibleDansLHistorique() {
        Departement departement = creerDepartement();
        FixtureManager manager = creerManagerAvecDepartement(departement);
        Employe membreEquipe = creerEmployeDuDepartement(departement);
        String outilReference = referenceDe(creerOutil(manager.token()));
        restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-out"), HttpMethod.POST,
                new HttpEntity<>(Map.of("employeId", membreEquipe.getId().toString()), authHeaders(manager.token())), Map.class);

        ResponseEntity<Map> checkIn = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/check-in"), HttpMethod.POST,
                new HttpEntity<>(Map.of("etat", "PERDU"), authHeaders(manager.token())), Map.class);

        assertThat(checkIn.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((Double) dataDe(checkIn).get("montantImputation")).isGreaterThan(0.0);

        ResponseEntity<Map> historique = restTemplate.exchange(baseUrl("/outillages/" + outilReference + "/historique"),
                HttpMethod.GET, new HttpEntity<>(authHeaders(manager.token())), Map.class);
        assertThat((List<?>) historique.getBody().get("content")).hasSize(2);
    }

    @Test
    void lister_parRh_voitLesOutilsDeTousLesDepartements() {
        FixtureManager managerA = creerManagerAvecDepartement(creerDepartement());
        FixtureManager managerB = creerManagerAvecDepartement(creerDepartement());
        creerOutil(managerA.token());
        creerOutil(managerB.token());
        String rhToken = login(createFixtureUser(Role.ROLE_RH));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/outillages"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rhToken)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<?>) reponse.getBody().get("content")).size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void lister_parManager_neVoitQueLesOutilsDeSonPropreDepartement() {
        FixtureManager managerA = creerManagerAvecDepartement(creerDepartement());
        FixtureManager managerB = creerManagerAvecDepartement(creerDepartement());
        creerOutil(managerA.token());
        creerOutil(managerB.token());

        ResponseEntity<Map> vuParA = restTemplate.exchange(baseUrl("/outillages"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerA.token())), Map.class);

        assertThat((List<?>) vuParA.getBody().get("content")).hasSize(1);
    }

    // Trouvé en revue de code : createFixtureUser(ROLE_MANAGER) laisse
    // departement_id=null (même situation que AssiduiteControllerIT). Sans
    // garde explicite, departementEffectif=null tombait sur le même chemin
    // que "RH sans filtre" (findAll — tout le parc) au lieu d'une liste vide.
    @Test
    void lister_parManagerSansDepartementAssigne_neVoitAucunOutil() {
        FixtureManager managerAvecOutil = creerManagerAvecDepartement(creerDepartement());
        creerOutil(managerAvecOutil.token());
        String managerSansDepartement = login(createFixtureUser(Role.ROLE_MANAGER));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/outillages"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerSansDepartement)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) reponse.getBody().get("content")).isEmpty();
    }

    @Test
    void creer_parManagerSansDepartementAssigne_estRefuseProprementSans500() {
        String managerSansDepartement = login(createFixtureUser(Role.ROLE_MANAGER));

        ResponseEntity<Map> reponse = creerOutil(managerSansDepartement);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
