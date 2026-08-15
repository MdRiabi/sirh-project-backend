package com.sirh.controller;

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

import java.util.List;
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
    public ResponseEntity<EvaluationDto> creer(@Valid @RequestBody EvaluationCreateRequest request,
                                                @AuthenticationPrincipal SirhUserDetails principal) {
        EvaluationDto dto = evaluationService.creer(request.employeId(), principal.getEmployeId(),
                principal.getDepartementId(), request.competences());
        return ResponseEntity.status(201).body(dto);
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<EvaluationDto> mettreAJour(@PathVariable Long id,
                                                       @Valid @RequestBody EvaluationUpdateRequest request,
                                                       @AuthenticationPrincipal SirhUserDetails principal) {
        EvaluationDto dto = evaluationService.mettreAJour(id, principal.getEmployeId(), request.competences());
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    @PutMapping("/{id}/valider")
    public ResponseEntity<EvaluationDto> valider(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.valider(id));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    @GetMapping("/{employeId}")
    public ResponseEntity<List<EvaluationDto>> parEmploye(@PathVariable UUID employeId,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        boolean restreint = principal.getRole() == Role.ROLE_MANAGER;
        return ResponseEntity.ok(evaluationService.parEmploye(employeId, principal.getEmployeId(),
                principal.getDepartementId(), restreint));
    }

    // 🆕 non listé explicitement au §4.10 : nécessaire pour l'écran de
    // validation RH (découvrir les grilles EN_COURS sans connaître l'employé
    // à l'avance) — même logique que GET /taches-renouvellement?statut=
    // (Lot 9). À reporter dans cahier-technique.md.
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    @GetMapping
    public ResponseEntity<List<EvaluationDto>> search(@RequestParam(required = false) EvaluationStatut statut,
                                                        @RequestParam(required = false) Integer annee,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        boolean restreint = principal.getRole() == Role.ROLE_MANAGER;
        return ResponseEntity.ok(evaluationService.search(statut, annee, principal.getDepartementId(), restreint));
    }
}
