package com.sirh.controller;

import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.ChartsDto;
import com.sirh.model.dto.KpiDto;
import com.sirh.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// cahier-technique.md §4.8/§2.2 : DIRECTEUR en lecture seule au même titre que
// MANAGER/RH/SUPERUSER, jamais de mutation exposée à ce rôle. MANAGER restreint
// à son propre département (même convention que EvaluationController), les
// trois autres rôles voient l'agrégat global.
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_DIRECTEUR')")
    @GetMapping("/kpi")
    public ResponseEntity<SingleObjectDto<KpiDto>> kpi(@AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(SingleObjectDto.of(dashboardService.kpi(scopeDepartementId(principal))));
    }

    // 🆕 non listé au §4.8 (seul /dashboard/kpi y figure) — ajouté pour les
    // visualisations du Tableau de Bord (CDF Module 10), cf. ChartsDto.
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_DIRECTEUR')")
    @GetMapping("/charts")
    public ResponseEntity<SingleObjectDto<ChartsDto>> charts(@AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(SingleObjectDto.of(dashboardService.charts(scopeDepartementId(principal))));
    }

    private Long scopeDepartementId(SirhUserDetails principal) {
        return principal.getRole() == Role.ROLE_MANAGER ? principal.getDepartementId() : null;
    }
}
