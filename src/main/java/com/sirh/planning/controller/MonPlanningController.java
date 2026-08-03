package com.sirh.planning.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.planning.dto.PlanningDto;
import com.sirh.planning.entity.Planning;
import com.sirh.planning.entity.Shift;
import com.sirh.planning.repository.ShiftRepository;
import com.sirh.planning.service.PlanningService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Consultation stricte de son propre planning (cahier-technique.md §4.4) : le
// filtre employe_id vient exclusivement du JWT, jamais d'un paramètre client.
@RestController
@RequestMapping("/mon-planning")
@PreAuthorize("hasAuthority('ROLE_EMPLOYE')")
public class MonPlanningController {

    private final PlanningService planningService;
    private final EmployeRepository employeRepository;
    private final ShiftRepository shiftRepository;

    public MonPlanningController(PlanningService planningService, EmployeRepository employeRepository,
                                  ShiftRepository shiftRepository) {
        this.planningService = planningService;
        this.employeRepository = employeRepository;
        this.shiftRepository = shiftRepository;
    }

    @GetMapping
    public ResponseEntity<List<PlanningDto>> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        List<Planning> plannings = planningService.findByEmploye(principal.getEmployeId(), dateDebut, dateFin);

        Employe employe = employeRepository.findById(principal.getEmployeId()).orElse(null);
        Map<Long, Shift> shiftsParId = shiftRepository
                .findAllById(plannings.stream().map(Planning::getShiftId).distinct().toList())
                .stream().collect(Collectors.toMap(Shift::getId, Function.identity()));

        List<PlanningDto> dtos = plannings.stream()
                .map(p -> PlanningDto.from(p,
                        employe != null ? employe.getNom() : null,
                        employe != null ? employe.getPrenom() : null,
                        shiftsParId.get(p.getShiftId()).getNom()))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}