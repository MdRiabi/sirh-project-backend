package com.sirh.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.entity.Contrat;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.TacheRenouvellement;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.enums.Role;
import com.sirh.model.enums.TacheRenouvellementStatut;
import com.sirh.model.repository.ContratRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.TacheRenouvellementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Couvre le workflow de traitement des tâches de renouvellement (Module 12, Lot 9) : accès
 * RH/SUPERUSER exclusif, URGENT_EXPIRE reste actionnable (arbitrage Lot 9), VALIDE renouvelle le
 * contrat, NON_RENOUVELE déclenche l'offboarding. La génération M+5/M+11 elle-même est couverte par
 * RenouvellementServiceTest (unitaire, sans Testcontainers) — ici les tâches sont insérées
 * directement pour isoler le comportement des endpoints.
 */
class TacheRenouvellementControllerIT extends AbstractIntegrationTest {

  @Autowired private EmployeRepository employeRepository;
  @Autowired private ContratRepository contratRepository;
  @Autowired private TacheRenouvellementRepository tacheRenouvellementRepository;

  private String accessTokenFor(Role role) {
    return login(createFixtureUser(role));
  }

  private String login(String username) {
    return login(username, FIXTURE_PASSWORD).getAccessToken();
  }

  private HttpHeaders authHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private Employe employeCdd() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    return employeRepository.save(
        Employe.builder()
            .matriculeInterne("IT-RENOUV-" + suffix)
            .nom("Test")
            .prenom("User")
            .typeContrat("CDD")
            .dateEmbauche(LocalDate.now().minusMonths(6))
            .actif(true)
            .build());
  }

  private Contrat contratCddEnCours(UUID employeId, LocalDate dateFin) {
    return contratRepository.save(
        Contrat.builder()
            .employeId(employeId)
            .type(ContratType.CDD)
            .dateDebut(LocalDate.now().minusMonths(6))
            .dateFin(dateFin)
            .statut(ContratStatut.EN_COURS)
            .build());
  }

  private TacheRenouvellement tache(
      UUID employeId, Long contratId, TacheRenouvellementStatut statut) {
    return tacheRenouvellementRepository.save(
        TacheRenouvellement.builder()
            .employeId(employeId)
            .contratId(contratId)
            .typeEcheance("M+5")
            .dateEcheance(LocalDate.now())
            .statut(statut)
            .build());
  }

  @Test
  void lister_parManager_estRefuseAvec403_gestionExclusiveRhSuperuser() {
    String managerToken = accessTokenFor(Role.ROLE_MANAGER);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(managerToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void lister_filtreParStatut_neRenvoieQueLesTachesDuStatutDemande() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    Contrat contrat = contratCddEnCours(employe.getId(), LocalDate.now().plusMonths(3));
    tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.EN_COURS);
    Contrat autreContrat = contratCddEnCours(employe.getId(), LocalDate.now().plusMonths(3));
    tache(employe.getId(), autreContrat.getId(), TacheRenouvellementStatut.VALIDE);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement?statut=EN_COURS"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<?> content = (List<?>) reponse.getBody().get("content");
    assertThat(content)
        .allSatisfy(item -> assertThat(((Map<?, ?>) item).get("statut")).isEqualTo("EN_COURS"));
  }

  @Test
  void traiter_valide_renouvelleLAncienContratEtEnCreeUnNouveauAvecLaDureeParDefaut() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    LocalDate ancienneFin = LocalDate.now().plusMonths(1);
    Contrat contrat = contratCddEnCours(employe.getId(), ancienneFin);
    TacheRenouvellement tache =
        tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.EN_COURS);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement/" + tache.getReference()),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("statut", "VALIDE"), authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) reponse.getBody().get("data")).get("statut")).isEqualTo("VALIDE");

    Contrat ancienRecharge = contratRepository.findById(contrat.getId()).orElseThrow();
    assertThat(ancienRecharge.getStatut()).isEqualTo(ContratStatut.RENOUVELE);

    List<Contrat> contratsEmploye =
        contratRepository.findByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(employe.getId());
    assertThat(contratsEmploye).hasSize(2);
    Contrat nouveau = contratsEmploye.get(0);
    assertThat(nouveau.getStatut()).isEqualTo(ContratStatut.EN_COURS);
    assertThat(nouveau.getDateDebut()).isEqualTo(ancienneFin);
    assertThat(nouveau.getDateFin()).isEqualTo(ancienneFin.plusMonths(6));
  }

  @Test
  void traiter_valideAvecDateFinExplicite_utiliseLaDateFournieAuLieuDeLaDureeParDefaut() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    LocalDate ancienneFin = LocalDate.now().plusMonths(1);
    Contrat contrat = contratCddEnCours(employe.getId(), ancienneFin);
    TacheRenouvellement tache =
        tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.URGENT_EXPIRE);
    LocalDate dateFinChoisie = ancienneFin.plusMonths(9);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement/" + tache.getReference()),
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("statut", "VALIDE", "dateFin", dateFinChoisie.toString()),
                authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Contrat nouveau =
        contratRepository
            .findByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(employe.getId())
            .get(0);
    assertThat(nouveau.getDateFin()).isEqualTo(dateFinChoisie);
  }

  @Test
  void traiter_nonRenouvele_termineLeContratEtDesactiveLEmploye() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    Contrat contrat = contratCddEnCours(employe.getId(), LocalDate.now().plusMonths(1));
    TacheRenouvellement tache =
        tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.EN_COURS);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement/" + tache.getReference()),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("statut", "NON_RENOUVELE"), authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Contrat contratRecharge = contratRepository.findById(contrat.getId()).orElseThrow();
    assertThat(contratRecharge.getStatut()).isEqualTo(ContratStatut.TERMINE);
    assertThat(contratRecharge.getMotifFin().name()).isEqualTo("FIN_CDD");

    Employe employeRecharge = employeRepository.findById(employe.getId()).orElseThrow();
    assertThat(employeRecharge.getActif()).isFalse();
  }

  @Test
  void traiter_urgentExpire_resteActionnable() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    Contrat contrat = contratCddEnCours(employe.getId(), LocalDate.now().minusDays(1));
    TacheRenouvellement tache =
        tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.URGENT_EXPIRE);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement/" + tache.getReference()),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("statut", "NON_RENOUVELE"), authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void traiter_tacheDejaValidee_estRefuseAvec409() {
    String rhToken = accessTokenFor(Role.ROLE_RH);
    Employe employe = employeCdd();
    Contrat contrat = contratCddEnCours(employe.getId(), LocalDate.now().plusMonths(1));
    TacheRenouvellement tache =
        tache(employe.getId(), contrat.getId(), TacheRenouvellementStatut.VALIDE);

    ResponseEntity<Map> reponse =
        restTemplate.exchange(
            baseUrl("/taches-renouvellement/" + tache.getReference()),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("statut", "NON_RENOUVELE"), authHeaders(rhToken)),
            Map.class);

    assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
