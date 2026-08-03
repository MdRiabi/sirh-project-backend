package com.sirh.planning.controller;

import com.sirh.auth.entity.Role;
import com.sirh.auth.security.SirhUserDetails;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.exception.EmployeNotFoundException;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.planning.dto.DeltaJourDto;
import com.sirh.planning.dto.PlanningAffecterRequest;
import com.sirh.planning.dto.PlanningDto;
import com.sirh.planning.entity.Planning;
import com.sirh.planning.entity.Shift;
import com.sirh.planning.repository.ShiftRepository;
import com.sirh.planning.service.PlanningService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/plannings")
public class PlanningController {

    private final PlanningService planningService;
    private final EmployeRepository employeRepository;
    private final ShiftRepository shiftRepository;

    public PlanningController(PlanningService planningService, EmployeRepository employeRepository,
                               ShiftRepository shiftRepository) {
        this.planningService = planningService;
        this.employeRepository = employeRepository;
        this.shiftRepository = shiftRepository;
    }

    @PostMapping("/affecter")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<PlanningDto> affecter(@Valid @RequestBody PlanningAffecterRequest request) {
        Planning planning = planningService.affecter(request.employeId(), request.shiftId(), request.dateDebut(),
                request.dateFin(), request.statut(), request.commentaire());
        return ResponseEntity.status(201).body(enrichir(List.of(planning)).get(0));
    }

    @GetMapping("/departement/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<List<PlanningDto>> parDepartement(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        verifierScopeManager(principal, id);
        List<Planning> plannings = planningService.findByDepartement(id, dateDebut, dateFin);
        return ResponseEntity.ok(enrichir(plannings));
    }

    @GetMapping("/delta")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    public ResponseEntity<List<DeltaJourDto>> delta(
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
            return ResponseEntity.ok(planningService.rapportDeltaDepartement(departementId, dateDebut, dateFin));
        }
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new EmployeNotFoundException(employeId));
        verifierScopeManager(principal, employe.getDepartementId());
        return ResponseEntity.ok(planningService.rapportDeltaEmploye(employeId, dateDebut, dateFin));
    }

    // Le Responsable Département ne consulte que le planning/delta de sa propre
    // équipe (cahier-fonctionnel.md Module 2, "consultation stricte").
    private void verifierScopeManager(SirhUserDetails principal, Long departementId) {
        boolean estManager = principal.getRole() == Role.ROLE_MANAGER;
        if (estManager && !Objects.equals(principal.getDepartementId(), departementId)) {
            throw new AccessDeniedException("Accès limité au planning de son propre département.");
        }
    }

    private List<PlanningDto> enrichir(List<Planning> plannings) {
        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(plannings.stream().map(Planning::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, Function.identity()));
        Map<Long, Shift> shiftsParId = shiftRepository
                .findAllById(plannings.stream().map(Planning::getShiftId).distinct().toList())
                .stream().collect(Collectors.toMap(Shift::getId, Function.identity()));

        return plannings.stream()
                .map(p -> {
                    Employe employe = employesParId.get(p.getEmployeId());
                    Shift shift = shiftsParId.get(p.getShiftId());
                    return PlanningDto.from(p,
                            employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            shift.getNom());
                })
                .toList();
    }
}