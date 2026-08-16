package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.EvaluationCreateRequest;
import com.sirh.model.dto.EvaluationDto;
import com.sirh.model.dto.EvaluationUpdateRequest;
import com.sirh.model.enums.EvaluationStatut;
import com.sirh.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Module 9 (cahier-technique.md §4.10) : "son équipe" pour MANAGER est résolu
// par département, cf. EvaluationService.
@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<SingleObjectDto<EvaluationDto>> creer(@Valid @RequestBody EvaluationCreateRequest request,
                                                @AuthenticationPrincipal SirhUserDetails principal) {
        EvaluationDto dto = evaluationService.creer(request.employeId(), principal.getEmployeId(),
                principal.getDepartementId(), request.competences());
        return ResponseEntity.status(201).body(SingleObjectDto.of(dto));
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{reference}")
    public ResponseEntity<SingleObjectDto<EvaluationDto>> mettreAJourEvaluationByReference(@PathVariable UUID reference,
                                                       @Valid @RequestBody EvaluationUpdateRequest request,
                                                       @AuthenticationPrincipal SirhUserDetails principal) {
        EvaluationDto dto = evaluationService.mettreAJourEvaluationByReference(reference, principal.getEmployeId(), request.competences());
        return ResponseEntity.ok(SingleObjectDto.of(dto));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    @PutMapping("/{reference}/valider")
    public ResponseEntity<SingleObjectDto<EvaluationDto>> validerEvaluationByReference(@PathVariable UUID reference) {
        return ResponseEntity.ok(SingleObjectDto.of(evaluationService.validerEvaluationByReference(reference)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    @GetMapping("/{employeId}")
    public ResponseEntity<ListingPageDto<EvaluationDto>> parEmploye(@PathVariable UUID employeId,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        boolean restreint = principal.getRole() == Role.ROLE_MANAGER;
        return ResponseEntity.ok(ListingPageDto.of(evaluationService.parEmploye(employeId, principal.getEmployeId(),
                principal.getDepartementId(), restreint)));
    }

    // 🆕 non listé explicitement au §4.10 : nécessaire pour l'écran de
    // validation RH (découvrir les grilles EN_COURS sans connaître l'employé
    // à l'avance) — même logique que GET /taches-renouvellement?statut=
    // (Lot 9). À reporter dans cahier-technique.md.
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    @GetMapping
    public ResponseEntity<ListingPageDto<EvaluationDto>> search(@RequestParam(required = false) EvaluationStatut statut,
                                                        @RequestParam(required = false) Integer annee,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        boolean restreint = principal.getRole() == Role.ROLE_MANAGER;
        return ResponseEntity.ok(ListingPageDto.of(evaluationService.search(statut, annee, principal.getDepartementId(), restreint)));
    }
}
