package com.sirh.controller;

import com.sirh.model.dto.CalculMensuelEmployeDto;
import com.sirh.model.dto.ClotureDto;
import com.sirh.model.dto.ClotureRequest;
import com.sirh.model.dto.DemandeDeverrouillageCreateRequest;
import com.sirh.model.dto.DemandeDeverrouillageDto;
import com.sirh.model.dto.DemandeDeverrouillageTraiterRequest;
import com.sirh.model.entity.DemandeDeverrouillage;
import com.sirh.service.AssiduiteCalculService;
import com.sirh.service.CloturePaieService;
import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
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

    // Lecture seule du statut de clôture, sans forcer la clôture ni exiger
    // qu'une ligne existe déjà (mois jamais clôturé -> OUVERT) — mêmes rôles
    // que calcul-mensuel, pas de scope département (la clôture est globale).
    @GetMapping("/cloture-paie")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<ClotureDto> statutCloture(@RequestParam String mois) {
        return ResponseEntity.ok(ClotureDto.from(cloturePaieService.trouverOuDefaut(YearMonth.parse(mois))));
    }

    @PostMapping("/cloturer-paie")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ClotureDto> cloturerPaie(@Valid @RequestBody ClotureRequest request,
                                                     @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(ClotureDto.from(
                cloturePaieService.cloturer(YearMonth.parse(request.mois()), principal.getEmployeId())));
    }

    @PostMapping("/demandes-deverrouillage")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<DemandeDeverrouillageDto> demanderDeverrouillage(
            @Valid @RequestBody DemandeDeverrouillageCreateRequest request,
            @AuthenticationPrincipal SirhUserDetails principal) {
        var demande = cloturePaieService.demanderDeverrouillage(YearMonth.parse(request.mois()), request.motif(), principal.getEmployeId());
        return ResponseEntity.status(201).body(cloturePaieService.enrichir(List.of(demande)).get(0));
    }

    // Ouvert à MANAGER (scope forcé à son propre département, même convention
    // que GET /autorisations) : nécessaire pour qu'il puisse savoir si une
    // demande EN_ATTENTE existe déjà pour le mois/département affiché (évite
    // les doublons de soumission côté frontend).
    @GetMapping("/demandes-deverrouillage")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<DemandeDeverrouillageDto>> listerDemandesDeverrouillage(
            @RequestParam(required = false) String mois,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) String statut,
            @AuthenticationPrincipal SirhUserDetails principal) {
        YearMonth moisParse = mois != null ? YearMonth.parse(mois) : null;
        List<DemandeDeverrouillage> resultats = principal.getRole() == Role.ROLE_MANAGER
                ? cloturePaieService.listerDemandesPourManager(moisParse, principal.getDepartementId(), statut)
                : cloturePaieService.listerDemandes(moisParse, departementId, statut);
        return ResponseEntity.ok(cloturePaieService.enrichir(resultats));
    }

    @PutMapping("/demandes-deverrouillage/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<DemandeDeverrouillageDto> traiterDemandeDeverrouillage(
            @PathVariable Long id, @Valid @RequestBody DemandeDeverrouillageTraiterRequest request,
            @AuthenticationPrincipal SirhUserDetails principal) {
        var demande = cloturePaieService.traiterDemande(id, request.statut(), principal.getEmployeId());
        return ResponseEntity.ok(cloturePaieService.enrichir(List.of(demande)).get(0));
    }
}
