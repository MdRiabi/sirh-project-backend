package com.sirh.controller;

import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.IndisponibiliteCreateRequest;
import com.sirh.model.dto.IndisponibiliteDto;
import com.sirh.model.dto.IndisponibiliteValiderRequest;
import com.sirh.model.dto.SoldeCongesDto;
import com.sirh.model.entity.Indisponibilite;
import com.sirh.service.IndisponibiliteService;
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
@RequestMapping("/indisponibilites")
public class IndisponibiliteController {

    private final IndisponibiliteService indisponibiliteService;

    public IndisponibiliteController(IndisponibiliteService indisponibiliteService) {
        this.indisponibiliteService = indisponibiliteService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<IndisponibiliteDto> creer(@Valid @RequestBody IndisponibiliteCreateRequest request,
                                                      @AuthenticationPrincipal SirhUserDetails principal) {
        String creeParRole = roleDemande(principal);
        Indisponibilite indisponibilite = indisponibiliteService.creer(
                request.employeId(), request.typeIndisponibilite(), request.dateDebut(), request.dateFin(),
                request.commentaire(), request.pieceJointeId(), principal.getEmployeId(), creeParRole,
                request.avisManagerStatut(), request.avisManagerCommentaire(), Boolean.TRUE.equals(request.forcer()));
        return ResponseEntity.status(201).body(indisponibiliteService.enrichirAvecDecompte(indisponibilite));
    }

    @PutMapping("/{reference}/valider")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<IndisponibiliteDto> validerIndisponibiliteByReference(@PathVariable UUID reference,
                                                        @RequestBody(required = false) IndisponibiliteValiderRequest request,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        String bascule = request != null ? request.bascule() : null;
        boolean forcer = request != null && Boolean.TRUE.equals(request.forcer());
        Indisponibilite indisponibilite = indisponibiliteService.validerIndisponibiliteByReference(reference, bascule, forcer, principal.getEmployeId());
        return ResponseEntity.ok(indisponibiliteService.enrichirAvecDecompte(indisponibilite));
    }

    // Gap symétrique comblé (documenté §4.6 pour /autorisations/{reference}/refuser
    // uniquement — absent côté indisponibilités alors que le workflow RH est
    // identique, valider/refuser, sur les deux modules).
    @PutMapping("/{reference}/refuser")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<IndisponibiliteDto> refuserIndisponibiliteByReference(@PathVariable UUID reference) {
        Indisponibilite indisponibilite = indisponibiliteService.refuserIndisponibiliteByReference(reference);
        return ResponseEntity.ok(indisponibiliteService.enrichirAvecDecompte(indisponibilite));
    }

    @GetMapping("/solde/{employeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<SoldeCongesDto> solde(@PathVariable UUID employeId) {
        return ResponseEntity.ok(new SoldeCongesDto(employeId, indisponibiliteService.solde(employeId)));
    }

    // Gap comblé (cf. /autorisations, /plannings/delta, /calendrier/jours-feries).
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<IndisponibiliteDto>> list(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID employeId,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        List<Indisponibilite> resultats = principal.getRole() == Role.ROLE_MANAGER
                ? indisponibiliteService.searchPourManager(principal.getDepartementId(), statut, dateDebut, dateFin)
                : indisponibiliteService.search(statut, employeId, departementId, dateDebut, dateFin);
        return ResponseEntity.ok(indisponibiliteService.enrichir(resultats));
    }

    private String roleDemande(SirhUserDetails principal) {
        return principal.getRole() == Role.ROLE_MANAGER ? "MANAGER" : "RH";
    }
}
