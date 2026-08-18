package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.EvaluationCreateRequest;
import com.sirh.model.dto.EvaluationDto;
import com.sirh.model.dto.EvaluationUpdateRequest;
import com.sirh.model.enums.EvaluationStatut;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Module 9 (cahier-technique.md §4.10) : "son équipe" pour MANAGER est résolu
// par département, cf. EvaluationService.
@Tag(
    name = "Évaluations annuelles",
    description =
        "Grille d'évaluation annuelle (Module 9, cahier-technique.md §4.10) — aucun accès"
            + " EMPLOYE. MANAGER crée/complète la grille de son équipe, RH/SUPERUSER valide.")
@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

  private final EvaluationService evaluationService;

  @Operation(
      summary = "Créer une grille d'évaluation",
      description =
          "Crée la grille de l'année en cours pour un membre de l'équipe du MANAGER appelant.")
  @PreAuthorize("hasAuthority('ROLE_MANAGER')")
  @PostMapping
  public ResponseEntity<SingleObjectDto<EvaluationDto>> creerEvaluation(
      @Valid @RequestBody EvaluationCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    EvaluationDto dto =
        evaluationService.creerEvaluation(
            request.employeId(),
            principal.getEmployeId(),
            principal.getDepartementId(),
            request.competences());
    return ResponseEntity.status(201).body(SingleObjectDto.of(dto));
  }

  @Operation(
      summary = "Compléter/corriger une grille d'évaluation",
      description = "**400 si une compétence obligatoire n'est pas notée** avant soumission.")
  @PreAuthorize("hasAuthority('ROLE_MANAGER')")
  @PutMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<EvaluationDto>> mettreAJourEvaluationByReference(
      @PathVariable UUID reference,
      @Valid @RequestBody EvaluationUpdateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    EvaluationDto dto =
        evaluationService.mettreAJourEvaluationByReference(
            reference, principal.getEmployeId(), request.competences());
    return ResponseEntity.ok(SingleObjectDto.of(dto));
  }

  @Operation(
      summary = "Valider une grille d'évaluation",
      description =
          "Génère automatiquement les lignes `plans_developpement` pour chaque compétence"
              + " sous le seuil d'exigence.")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @PutMapping("/{reference}/valider")
  public ResponseEntity<SingleObjectDto<EvaluationDto>> validerEvaluationByReference(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(evaluationService.validerEvaluationByReference(reference)));
  }

  @Operation(
      summary = "Lister les évaluations d'un employé",
      description = "MANAGER : restreint à son équipe (département).")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  @GetMapping("/{employeId}")
  public ResponseEntity<ListingPageDto<EvaluationDto>> parEmploye(
      @PathVariable UUID employeId, @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            evaluationService.parEmployeAvecScope(
                employeId,
                principal.getRole(),
                principal.getEmployeId(),
                principal.getDepartementId())));
  }

  // 🆕 non listé explicitement au §4.10 : nécessaire pour l'écran de
  // validation RH (découvrir les grilles EN_COURS sans connaître l'employé
  // à l'avance) — même logique que GET /taches-renouvellement?statut=
  // (Lot 9). À reporter dans cahier-technique.md.
  @Operation(
      summary = "Rechercher des évaluations",
      description =
          "Nécessaire pour l'écran de validation RH (file `statut=EN_COURS`) sans connaître"
              + " les employés à l'avance.")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  @GetMapping
  public ResponseEntity<ListingPageDto<EvaluationDto>> search(
      @RequestParam(required = false) EvaluationStatut statut,
      @RequestParam(required = false) Integer annee,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            evaluationService.searchAvecScope(
                statut, annee, principal.getRole(), principal.getDepartementId())));
  }
}
