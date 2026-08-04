package com.sirh.assiduite.controller;

import com.sirh.assiduite.dto.CalculMensuelEmployeDto;
import com.sirh.assiduite.dto.ClotureDto;
import com.sirh.assiduite.dto.ClotureRequest;
import com.sirh.assiduite.dto.DemandeDeverrouillageCreateRequest;
import com.sirh.assiduite.dto.DemandeDeverrouillageDto;
import com.sirh.assiduite.dto.DemandeDeverrouillageTraiterRequest;
import com.sirh.assiduite.service.AssiduiteCalculService;
import com.sirh.assiduite.service.CloturePaieService;
import com.sirh.auth.entity.Role;
import com.sirh.auth.security.SirhUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/assiduite")
public class AssiduiteController {

    private final AssiduiteCalculService assiduiteCalculService;
    private final CloturePaieService cloturePaieService;

    public AssiduiteController(AssiduiteCalculService assiduiteCalculService, CloturePaieService cloturePaieService) {
        this.assiduiteCalculService = assiduiteCalculService;
        this.cloturePaieService = cloturePaieService;
    }

    // MANAGER restreint à son propre département, filtres client ignorés — même
    // convention que GET /autorisations, GET /indisponibilites (Lot 5).
    @GetMapping("/calcul-mensuel")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<List<CalculMensuelEmployeDto>> calculMensuel(
            @RequestParam String mois,
            @RequestParam(required = false) Long departementId,
            @AuthenticationPrincipal SirhUserDetails principal) {
        Long departementEffectif = principal.getRole() == Role.ROLE_MANAGER ? principal.getDepartementId() : departementId;
        return ResponseEntity.ok(assiduiteCalculService.calculerMois(YearMonth.parse(mois), departementEffectif));
    }

    @PostMapping("/cloturer-paie")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ClotureDto> cloturerPaie(@Valid @RequestBody ClotureRequest request,
                                                     @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(ClotureDto.from(cloturePaieService.cloturer(request.mois(), principal.getEmployeId())));
    }

    @PostMapping("/demandes-deverrouillage")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<DemandeDeverrouillageDto> demanderDeverrouillage(
            @Valid @RequestBody DemandeDeverrouillageCreateRequest request,
            @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.status(201).body(DemandeDeverrouillageDto.from(
                cloturePaieService.demanderDeverrouillage(request.mois(), request.motif(), principal.getEmployeId())));
    }

    @GetMapping("/demandes-deverrouillage")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<DemandeDeverrouillageDto>> listerDemandesDeverrouillage(
            @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(cloturePaieService.listerDemandes(statut).stream().map(DemandeDeverrouillageDto::from).toList());
    }

    @PutMapping("/demandes-deverrouillage/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<DemandeDeverrouillageDto> traiterDemandeDeverrouillage(
            @PathVariable Long id, @Valid @RequestBody DemandeDeverrouillageTraiterRequest request,
            @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(DemandeDeverrouillageDto.from(
                cloturePaieService.traiterDemande(id, request.statut(), principal.getEmployeId())));
    }
}
