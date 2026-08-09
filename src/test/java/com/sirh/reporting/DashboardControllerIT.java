package com.sirh.reporting;

import com.sirh.AbstractIntegrationTest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.personnel.entity.Departement;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.DepartementRepository;
import com.sirh.personnel.repository.EmployeRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Module 10 (Lot 11) : accès par rôle (EMPLOYE exclu, DIRECTEUR lecture seule
// globale, MANAGER restreint à son propre département via SirhUserDetails)
// et exactitude de l'agrégation native (EmployeRepository) sur un jeu de
// données Testcontainers réel plutôt qu'en Mockito — les requêtes SQL/Postgres
// (generate_series) ne se vérifient pas correctement avec des mocks.
class DashboardControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DepartementRepository departementRepository;
    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Departement creerDepartement() {
        return departementRepository.save(Departement.builder().nom("Dept-" + UUID.randomUUID().toString().substring(0, 8)).build());
    }

    private Employe creerEmploye(Departement departement) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return employeRepository.save(Employe.builder()
                .matriculeInterne("IT-" + suffix).nom("Test").prenom("Employe").typeContrat("CDI")
                .dateEmbauche(LocalDate.now().minusYears(1)).actif(true).departement(departement).build());
    }

    private String creerUtilisateur(Role role, Employe employe) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "it-" + role.name().toLowerCase() + "-" + suffix;
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe).username(username).passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(role).compteActif(true).echecsLoginConsecutifs(0).build());
        return login(username);
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

    @Test
    void kpi_employe_estRefuseAvec403() {
        Departement departement = creerDepartement();
        String token = creerUtilisateur(Role.ROLE_EMPLOYE, creerEmploye(departement));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/dashboard/kpi"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void kpi_directeur_voitLAgregatGlobal() {
        Departement departement = creerDepartement();
        creerEmploye(departement);
        creerEmploye(departement);
        String token = creerUtilisateur(Role.ROLE_DIRECTEUR, creerEmploye(creerDepartement()));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/dashboard/kpi"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) reponse.getBody().get("effectifTotal")).longValue()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void kpi_manager_estRestreintAuPropreDepartement() {
        Departement departementManager = creerDepartement();
        creerEmploye(departementManager);
        creerEmploye(departementManager);
        Employe employeManager = creerEmploye(departementManager);
        String tokenManager = creerUtilisateur(Role.ROLE_MANAGER, employeManager);

        Departement autreDepartement = creerDepartement();
        creerEmploye(autreDepartement);
        creerEmploye(autreDepartement);
        creerEmploye(autreDepartement);
        creerEmploye(autreDepartement);

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/dashboard/kpi"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenManager)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3 employés dans le département du Manager (2 + lui-même), jamais les 4
        // de l'autre département malgré leur présence en base.
        assertThat(((Number) reponse.getBody().get("effectifTotal")).longValue()).isEqualTo(3);
    }

    @Test
    void charts_rh_renvoieDouzeMoisEtHuitSemaines() {
        String token = creerUtilisateur(Role.ROLE_RH, creerEmploye(creerDepartement()));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/dashboard/charts"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((java.util.List<?>) reponse.getBody().get("evolutionEffectifs")).hasSize(12);
        assertThat((java.util.List<?>) reponse.getBody().get("turnoverMensuel")).hasSize(12);
        assertThat((java.util.List<?>) reponse.getBody().get("anomaliesPointageHebdo")).hasSize(8);
    }
}
