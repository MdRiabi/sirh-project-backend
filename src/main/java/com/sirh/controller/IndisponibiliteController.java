package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.IndisponibiliteCreateRequest;
import com.sirh.model.dto.IndisponibiliteDto;
import com.sirh.model.dto.IndisponibiliteValiderRequest;
import com.sirh.model.dto.SoldeCongesDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.IndisponibiliteService;
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
@RequestMapping("/indisponibilites")
@RequiredArgsConstructor
public class IndisponibiliteController {

  private final IndisponibiliteService indisponibiliteService;

  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<IndisponibiliteDto>> creer(
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
  @PutMapping("/{reference}/refuser")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<IndisponibiliteDto>> refuserIndisponibiliteByReference(
      @PathVariable UUID reference) {
    IndisponibiliteDto indisponibilite =
        indisponibiliteService.refuserIndisponibiliteByReference(reference);
    return ResponseEntity.ok(SingleObjectDto.of(indisponibilite));
  }

  @GetMapping("/solde/{employeId}")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<SoldeCongesDto>> solde(@PathVariable UUID employeId) {
    return ResponseEntity.ok(
        SingleObjectDto.of(new SoldeCongesDto(employeId, indisponibiliteService.solde(employeId))));
  }

  // Gap comblé (cf. /autorisations, /plannings/delta, /calendrier/jours-feries).
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<IndisponibiliteDto>> list(
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
