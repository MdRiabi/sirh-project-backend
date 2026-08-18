package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.AutorisationCreateRequest;
import com.sirh.model.dto.AutorisationDto;
import com.sirh.model.dto.AutorisationValiderRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.AutorisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Autorisations",
    description =
        "Absences courtes (retards, sorties) — **aucun accès EMPLOYE**, saisie exclusive"
            + " MANAGER/RH/SUPERUSER (cahier-technique.md §4.6). Le MANAGER capture la demande"
            + " au nom de l'employé avec avis obligatoire ; la validation finale RH est une"
            + " étape distincte.")
@RestController
@RequestMapping("/autorisations")
@RequiredArgsConstructor
public class AutorisationController {

  private final AutorisationService autorisationService;

  @Operation(
      summary = "Créer une autorisation d'absence",
      description =
          "`avisManagerStatut` obligatoire si le créateur est MANAGER (ignoré si RH/SUPERUSER)."
              + " Statut initial `EN_ATTENTE_RH` côté MANAGER — **409 si"
              + " `dateAutorisation`+`heureDebut` dépasse 72h dans le passé**. Côté RH/SUPERUSER"
              + " (voie urgence), statut `VALIDE` directement : justification d'anomalie de"
              + " pointage et mise à jour du planning appliquées immédiatement, sans passer par"
              + " `valider()`.")
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<AutorisationDto>> creerAutorisation(
      @Valid @RequestBody AutorisationCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    AutorisationDto autorisation =
        autorisationService.creerAutorisation(
            request.employeId(),
            request.type(),
            request.dateAutorisation(),
            request.heureDebut(),
            request.heureFin(),
            request.dureeMinutes(),
            request.motif(),
            principal.getEmployeId(),
            principal.getRole(),
            request.avisManagerStatut(),
            request.avisManagerCommentaire());
    return ResponseEntity.status(201).body(SingleObjectDto.of(autorisation));
  }

  @Operation(
      summary = "Valider une autorisation",
      description =
          "Requiert le statut `EN_ATTENTE_RH` (409 sinon). Si le cumul mensuel de l'employé"
              + " (`EN_ATTENTE_RH`+`VALIDE`) dépasse 4h, `traitementDepassement`"
              + " (`DEDUCTION_SALAIRE`|`RECUPERATION`) est obligatoire dans le corps (409"
              + " sinon) — décision journalisée dans l'audit.")
  @PutMapping("/{reference}/valider")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<AutorisationDto>> validerAutorisationByReference(
      @PathVariable UUID reference,
      @RequestBody(required = false) AutorisationValiderRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    String traitementDepassement = request != null ? request.traitementDepassement() : null;
    AutorisationDto autorisation =
        autorisationService.validerAutorisationByReference(
            reference, traitementDepassement, principal.getEmployeId());
    return ResponseEntity.ok(SingleObjectDto.of(autorisation));
  }

  @Operation(
      summary = "Refuser une autorisation",
      description =
          "Requiert le statut `EN_ATTENTE_RH` (409 sinon). Aucun effet sur solde, pointage ou"
              + " planning.")
  @PutMapping("/{reference}/refuser")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<AutorisationDto>> refuserAutorisationByReference(
      @PathVariable UUID reference) {
    AutorisationDto autorisation = autorisationService.refuserAutorisationByReference(reference);
    return ResponseEntity.ok(SingleObjectDto.of(autorisation));
  }

  // Gap comblé (cf. /plannings/delta au Lot 3, /calendrier/jours-feries au
  // Lot 4) : cahier-technique.md §4.6 ne listait aucun endpoint de listing,
  // pourtant requis par BACKLOG.md pour les écrans "liste à valider"/"historique".
  @Operation(
      summary = "Lister les autorisations",
      description =
          "MANAGER : restreint automatiquement à son propre département, le filtre"
              + " `departementId` du client est ignoré.")
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<AutorisationDto>> list(
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) UUID employeId,
      @Parameter(description = "Ignoré si l'appelant est MANAGER (scope forcé à son département)")
          @RequestParam(required = false)
          Long departementId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dateDebut,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dateFin,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            autorisationService.searchAvecScope(
                principal.getRole(),
                principal.getDepartementId(),
                statut,
                employeId,
                departementId,
                dateDebut,
                dateFin)));
  }
}
