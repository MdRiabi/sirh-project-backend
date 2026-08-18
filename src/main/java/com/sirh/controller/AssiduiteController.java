package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.CalculMensuelEmployeDto;
import com.sirh.model.dto.ClotureDto;
import com.sirh.model.dto.ClotureRequest;
import com.sirh.model.dto.DemandeDeverrouillageCreateRequest;
import com.sirh.model.dto.DemandeDeverrouillageDto;
import com.sirh.model.dto.DemandeDeverrouillageTraiterRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.AssiduiteCalculService;
import com.sirh.service.CloturePaieService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assiduite")
@RequiredArgsConstructor
public class AssiduiteController {

  private final AssiduiteCalculService assiduiteCalculService;
  private final CloturePaieService cloturePaieService;

  // MANAGER restreint à son propre département, filtres client ignorés — même
  // convention que GET /autorisations, GET /indisponibilites (Lot 5).
  @GetMapping("/calcul-mensuel")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<ListingPageDto<CalculMensuelEmployeDto>> calculMensuel(
      @RequestParam String mois,
      @RequestParam(required = false) Long departementId,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            assiduiteCalculService.calculerMoisAvecScope(
                YearMonth.parse(mois),
                principal.getRole(),
                principal.getDepartementId(),
                departementId)));
  }

  // Lecture seule du statut de clôture, sans forcer la clôture ni exiger
  // qu'une ligne existe déjà (mois jamais clôturé -> OUVERT) — mêmes rôles
  // que calcul-mensuel, pas de scope département (la clôture est globale).
  @GetMapping("/cloture-paie")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<SingleObjectDto<ClotureDto>> statutCloture(@RequestParam String mois) {
    return ResponseEntity.ok(
        SingleObjectDto.of(cloturePaieService.trouverOuDefaut(YearMonth.parse(mois))));
  }

  @PostMapping("/cloturer-paie")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<ClotureDto>> cloturerPaie(
      @Valid @RequestBody ClotureRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            cloturePaieService.cloturer(
                YearMonth.parse(request.mois()), principal.getEmployeId())));
  }

  @PostMapping("/demandes-deverrouillage")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<DemandeDeverrouillageDto>> demanderDeverrouillage(
      @Valid @RequestBody DemandeDeverrouillageCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var demande =
        cloturePaieService.demanderDeverrouillage(
            YearMonth.parse(request.mois()), request.motif(), principal.getEmployeId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(demande));
  }

  // Ouvert à MANAGER (scope forcé à son propre département, même convention
  // que GET /autorisations) : nécessaire pour qu'il puisse savoir si une
  // demande EN_ATTENTE existe déjà pour le mois/département affiché (évite
  // les doublons de soumission côté frontend).
  @GetMapping("/demandes-deverrouillage")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<DemandeDeverrouillageDto>> listerDemandesDeverrouillage(
      @RequestParam(required = false) String mois,
      @RequestParam(required = false) Long departementId,
      @RequestParam(required = false) String statut,
      @AuthenticationPrincipal SirhUserDetails principal) {
    YearMonth moisParse = mois != null ? YearMonth.parse(mois) : null;
    return ResponseEntity.ok(
        ListingPageDto.of(
            cloturePaieService.listerDemandesAvecScope(
                principal.getRole(),
                principal.getDepartementId(),
                moisParse,
                departementId,
                statut)));
  }

  @PutMapping("/demandes-deverrouillage/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<DemandeDeverrouillageDto>>
      traiterDemandeDeverrouillageByReference(
          @PathVariable UUID reference,
          @Valid @RequestBody DemandeDeverrouillageTraiterRequest request,
          @AuthenticationPrincipal SirhUserDetails principal) {
    var demande =
        cloturePaieService.traiterDemandeByReference(
            reference, request.statut(), principal.getEmployeId());
    return ResponseEntity.ok(SingleObjectDto.of(demande));
  }
}
