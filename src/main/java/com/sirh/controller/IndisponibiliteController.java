package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.IndisponibiliteCreateRequest;
import com.sirh.model.dto.IndisponibiliteDto;
import com.sirh.model.dto.IndisponibiliteValiderRequest;
import com.sirh.model.dto.SoldeCongesDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.IndisponibiliteService;
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
    name = "Indisponibilités",
    description =
        "Congés/absences longues — **aucun accès EMPLOYE**, saisie exclusive"
            + " MANAGER/RH/SUPERUSER (cahier-technique.md §4.6). Même workflow à 2 étapes"
            + " qu'Autorisations, mais sans délai de 72h ni quota (spécifiques aux"
            + " autorisations).")
@RestController
@RequestMapping("/indisponibilites")
@RequiredArgsConstructor
public class IndisponibiliteController {

  private final IndisponibiliteService indisponibiliteService;

  @Operation(
      summary = "Créer une indisponibilité",
      description =
          "`avisManagerStatut` obligatoire si le créateur est MANAGER (ignoré si"
              + " RH/SUPERUSER). `forcer` : passe-droit RH en urgence malgré un solde"
              + " insuffisant.")
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<IndisponibiliteDto>> creerIndisponibilite(
      @Valid @RequestBody IndisponibiliteCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    IndisponibiliteDto indisponibilite =
        indisponibiliteService.creerIndisponibilite(
            request.employeId(),
            request.typeIndisponibilite(),
            request.dateDebut(),
            request.dateFin(),
            request.commentaire(),
            request.pieceJointeId(),
            principal.getEmployeId(),
            principal.getRole(),
            request.avisManagerStatut(),
            request.avisManagerCommentaire(),
            Boolean.TRUE.equals(request.forcer()));
    return ResponseEntity.status(201).body(SingleObjectDto.of(indisponibilite));
  }

  @Operation(
      summary = "Valider une indisponibilité",
      description =
          "Requiert le statut `EN_ATTENTE_RH` (409 sinon). `bascule` change le"
              + " `type_indisponibilite` avant déduction (ex. vers `CONGE_SANS_SOLDE`)."
              + " `forcer` passe outre un solde insuffisant (**409 sinon : \"Solde insuffisant"
              + " (X jours disponibles)\"**). Déduction comptée en jours ouvrés (weekends et"
              + " jours `FERIE` exclus), uniquement si `type_indisponibilite = CONGE_PAYE`. À"
              + " la validation, le planning de l'employé (source de vérité) est mis à jour :"
              + " tout créneau existant chevauchant est supprimé et remplacé par un créneau"
              + " `CONGE`/`ABSENCE` ; une nouvelle affectation planning est refusée si elle"
              + " chevauche un congé déjà `VALIDE`.")
  @PutMapping("/{reference}/valider")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<IndisponibiliteDto>> validerIndisponibiliteByReference(
      @PathVariable UUID reference,
      @RequestBody(required = false) IndisponibiliteValiderRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    String bascule = request != null ? request.bascule() : null;
    boolean forcer = request != null && Boolean.TRUE.equals(request.forcer());
    IndisponibiliteDto indisponibilite =
        indisponibiliteService.validerIndisponibiliteByReference(
            reference, bascule, forcer, principal.getEmployeId());
    return ResponseEntity.ok(SingleObjectDto.of(indisponibilite));
  }

  // Gap symétrique comblé (documenté §4.6 pour /autorisations/{reference}/refuser
  // uniquement — absent côté indisponibilités alors que le workflow RH est
  // identique, valider/refuser, sur les deux modules).
  @Operation(
      summary = "Refuser une indisponibilité",
      description = "Requiert le statut `EN_ATTENTE_RH` (409 sinon).")
  @PutMapping("/{reference}/refuser")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<IndisponibiliteDto>> refuserIndisponibiliteByReference(
      @PathVariable UUID reference) {
    IndisponibiliteDto indisponibilite =
        indisponibiliteService.refuserIndisponibiliteByReference(reference);
    return ResponseEntity.ok(SingleObjectDto.of(indisponibilite));
  }

  @Operation(summary = "Consulter le solde de congés d'un employé")
  @GetMapping("/solde/{employeId}")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<SoldeCongesDto>> solde(@PathVariable UUID employeId) {
    return ResponseEntity.ok(
        SingleObjectDto.of(new SoldeCongesDto(employeId, indisponibiliteService.solde(employeId))));
  }

  // Gap comblé (cf. /autorisations, /plannings/delta, /calendrier/jours-feries).
  @Operation(
      summary = "Lister les indisponibilités",
      description =
          "MANAGER : restreint automatiquement à son propre département, le filtre"
              + " `departementId` du client est ignoré.")
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<IndisponibiliteDto>> list(
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
            indisponibiliteService.searchAvecScope(
                principal.getRole(),
                principal.getDepartementId(),
                statut,
                employeId,
                departementId,
                dateDebut,
                dateFin)));
  }
}
