package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.entity.Employe;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.dto.DeltaJourDto;
import com.sirh.model.dto.PlanningAffecterRequest;
import com.sirh.model.dto.PlanningDto;
import com.sirh.service.PlanningService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/plannings")
public class PlanningController {

    private final PlanningService planningService;
    private final EmployeRepository employeRepository;

    public PlanningController(PlanningService planningService, EmployeRepository employeRepository) {
        this.planningService = planningService;
        this.employeRepository = employeRepository;
    }

    @PostMapping("/affecter")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<SingleObjectDto<PlanningDto>> affecter(@Valid @RequestBody PlanningAffecterRequest request) {
        PlanningDto planning = planningService.affecter(request.employeId(), request.shiftId(), request.dateDebut(),
                request.dateFin(), request.statut(), request.commentaire());
        return ResponseEntity.status(201).body(SingleObjectDto.of(planning));
    }

    @GetMapping("/departement/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<ListingPageDto<PlanningDto>> parDepartement(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        verifierScopeManager(principal, id);
        return ResponseEntity.ok(ListingPageDto.of(planningService.findByDepartement(id, dateDebut, dateFin)));
    }

    @GetMapping("/delta")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<ListingPageDto<DeltaJourDto>> delta(
            @RequestParam(required = false) UUID employeId,
            @RequestParam(required = false) Long departementId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        if ((employeId == null) == (departementId == null)) {
            throw new IllegalArgumentException("Fournir exactement un des deux paramètres : employeId ou departementId.");
        }
        if (departementId != null) {
            verifierScopeManager(principal, departementId);
            return ResponseEntity.ok(ListingPageDto.of(planningService.rapportDeltaDepartement(departementId, dateDebut, dateFin)));
        }
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new EmployeNotFoundException(employeId));
        verifierScopeManager(principal, employe.getDepartementId());
        return ResponseEntity.ok(ListingPageDto.of(planningService.rapportDeltaEmploye(employeId, dateDebut, dateFin)));
    }

    // Le Responsable Département ne consulte que le planning/delta de sa propre
    // équipe (cahier-fonctionnel.md Module 2, "consultation stricte").
    private void verifierScopeManager(SirhUserDetails principal, Long departementId) {
        boolean estManager = principal.getRole() == Role.ROLE_MANAGER;
        if (estManager && !Objects.equals(principal.getDepartementId(), departementId)) {
            throw new AccessDeniedException("Accès limité au planning de son propre département.");
        }
    }
}
