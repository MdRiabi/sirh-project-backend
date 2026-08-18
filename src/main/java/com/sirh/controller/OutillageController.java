package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.CheckInRequest;
import com.sirh.model.dto.CheckOutRequest;
import com.sirh.model.dto.OutillageCreateRequest;
import com.sirh.model.dto.OutillageDto;
import com.sirh.model.dto.OutillageMouvementDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.OutillageService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Gestion du matériel (cahier-fonctionnel.md Module 5) : rattachement/
// restitution exclusifs au Responsable Département de l'outil ("gestion
// exclusivement effectuée par le Responsable Département") — RH/SUPERUSER
// restent en lecture seule (visibilité pour le traitement financier des
// pertes déclarées, GET /outillages, GET /outillages/{id}/historique).
@RestController
@RequestMapping("/outillages")
@RequiredArgsConstructor
public class OutillageController {

  private final OutillageService outillageService;

  @PostMapping
  @PreAuthorize("hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<SingleObjectDto<OutillageDto>> creerOutillage(
      @Valid @RequestBody OutillageCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    OutillageDto outillage =
        outillageService.creerOutillage(
            request.codeBarres(),
            request.nomOutil(),
            request.valeurAchat(),
            request.dateAchat(),
            principal.getDepartementId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(outillage));
  }

  // MANAGER restreint à son propre département (departementId client ignoré),
  // même convention que GET /autorisations, GET /assiduite/calcul-mensuel.
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<OutillageDto>> lister(
      @RequestParam(required = false) Long departementId,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            outillageService.listerAvecScope(
                principal.getRole(), principal.getDepartementId(), departementId)));
  }

  @PostMapping("/{reference}/check-out")
  @PreAuthorize("hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<SingleObjectDto<OutillageMouvementDto>> checkOut(
      @PathVariable UUID reference,
      @Valid @RequestBody CheckOutRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var mouvement =
        outillageService.checkOutParReference(
            reference, request.employeId(), principal.getUserId(), principal.getDepartementId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(mouvement));
  }

  @PostMapping("/{reference}/check-in")
  @PreAuthorize("hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<SingleObjectDto<OutillageMouvementDto>> checkIn(
      @PathVariable UUID reference,
      @Valid @RequestBody CheckInRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var mouvement =
        outillageService.checkInParReference(
            reference,
            request.etat(),
            request.commentaire(),
            principal.getUserId(),
            principal.getDepartementId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(mouvement));
  }

  @GetMapping("/{reference}/historique")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<OutillageMouvementDto>> historique(
      @PathVariable UUID reference, @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            outillageService.historiqueParReference(
                reference, principal.getRole(), principal.getDepartementId())));
  }
}
