package com.sirh.controller;

import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.AutorisationCreateRequest;
import com.sirh.model.dto.AutorisationDto;
import com.sirh.model.dto.AutorisationValiderRequest;
import com.sirh.model.entity.Autorisation;
import com.sirh.service.AutorisationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/autorisations")
public class AutorisationController {

    private final AutorisationService autorisationService;

    public AutorisationController(AutorisationService autorisationService) {
        this.autorisationService = autorisationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<AutorisationDto> creer(@Valid @RequestBody AutorisationCreateRequest request,
                                                  @AuthenticationPrincipal SirhUserDetails principal) {
        String creeParRole = roleDemande(principal);
        Autorisation autorisation = autorisationService.creer(
                request.employeId(), request.type(), request.dateAutorisation(), request.heureDebut(),
                request.heureFin(), request.dureeMinutes(), request.motif(), principal.getEmployeId(), creeParRole,
                request.avisManagerStatut(), request.avisManagerCommentaire());
        return ResponseEntity.status(201).body(autorisationService.enrichirAvecQuota(autorisation));
    }

    @PutMapping("/{reference}/valider")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<AutorisationDto> validerAutorisationByReference(@PathVariable UUID reference,
                                                     @RequestBody(required = false) AutorisationValiderRequest request,
                                                     @AuthenticationPrincipal SirhUserDetails principal) {
        String traitementDepassement = request != null ? request.traitementDepassement() : null;
        Autorisation autorisation = autorisationService.validerAutorisationByReference(reference, traitementDepassement, principal.getEmployeId());
        return ResponseEntity.ok(autorisationService.enrichirAvecQuota(autorisation));
    }

    @PutMapping("/{reference}/refuser")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<AutorisationDto> refuserAutorisationByReference(@PathVariable UUID reference) {
        Autorisation autorisation = autorisationService.refuserAutorisationByReference(reference);
        return ResponseEntity.ok(autorisationService.enrichirAvecQuota(autorisation));
    }

    // Gap comblé (cf. /plannings/delta au Lot 3, /calendrier/jours-feries au
    // Lot 4) : cahier-technique.md §4.6 ne listait aucun endpoint de listing,
    // pourtant requis par BACKLOG.md pour les écrans "liste à valider"/"historique".
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<AutorisationDto>> list(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID employeId,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        List<Autorisation> resultats = principal.getRole() == Role.ROLE_MANAGER
                ? autorisationService.searchPourManager(principal.getDepartementId(), statut, dateDebut, dateFin)
                : autorisationService.search(statut, employeId, departementId, dateDebut, dateFin);
        return ResponseEntity.ok(autorisationService.enrichir(resultats));
    }

    // cree_par_role (schéma) n'accepte que MANAGER/RH — le SUPERUSER, qui a les
    // mêmes droits que le RH (cahier-technique.md §2.1), est tracé comme "RH".
    private String roleDemande(SirhUserDetails principal) {
        return principal.getRole() == Role.ROLE_MANAGER ? "MANAGER" : "RH";
    }
}
