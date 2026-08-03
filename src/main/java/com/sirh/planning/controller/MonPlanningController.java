package com.sirh.planning.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.planning.dto.PlanningDto;
import com.sirh.planning.entity.Planning;
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

// Consultation stricte de son propre planning (cahier-technique.md §4.4) : le
// filtre employe_id vient exclusivement du JWT, jamais d'un paramètre client.
@RestController
@RequestMapping("/mon-planning")
@PreAuthorize("hasAuthority('ROLE_EMPLOYE')")
public class MonPlanningController {

    private final PlanningService planningService;

    public MonPlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping
    public ResponseEntity<List<PlanningDto>> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @AuthenticationPrincipal SirhUserDetails principal) {
        List<Planning> plannings = planningService.findByEmploye(principal.getEmployeId(), dateDebut, dateFin);
        return ResponseEntity.ok(planningService.enrichir(plannings));
    }
}
