package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.AutorisationCreateRequest;
import com.sirh.model.dto.AutorisationDto;
import com.sirh.model.dto.AutorisationValiderRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.AutorisationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/autorisations")
@RequiredArgsConstructor
public class AutorisationController {

  private final AutorisationService autorisationService;

  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<AutorisationDto>> creer(
      @Valid @RequestBody AutorisationCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    AutorisationDto autorisation =
        autorisationService.creer(
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
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<AutorisationDto>> list(
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) UUID employeId,
      @RequestParam(required = false) Long departementId,
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
