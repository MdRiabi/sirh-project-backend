package com.sirh.controller;

import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ChartsDto;
import com.sirh.model.dto.KpiDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@Tag(
    name = "Tableau de bord",
    description =
        "KPI et graphiques agrégés (cache Redis, TTL 30-60s) — MANAGER restreint à son propre"
            + " département, RH/SUPERUSER/DIRECTEUR voient l'agrégat global (cahier-technique.md"
            + " §4.8)")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @Operation(
      summary = "Consulter les KPI du tableau de bord",
      description = "MANAGER restreint à son propre département, sinon agrégat global.")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_DIRECTEUR')")
  @GetMapping("/kpi")
  public ResponseEntity<SingleObjectDto<KpiDto>> kpi(
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            dashboardService.kpiAvecScope(principal.getRole(), principal.getDepartementId())));
  }

  // 🆕 non listé au §4.8 (seul /dashboard/kpi y figure) — ajouté pour les
  // visualisations du Tableau de Bord (CDF Module 10), cf. ChartsDto.
  @Operation(
      summary = "Consulter les graphiques du tableau de bord",
      description =
          "`evolutionEffectifs`/`turnoverMensuel` (12 derniers mois glissants), `congesEnCours`,"
              + " `anomaliesPointageHebdo` (8 dernières semaines). MANAGER restreint à son propre"
              + " département, sinon agrégat global.")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_DIRECTEUR')")
  @GetMapping("/charts")
  public ResponseEntity<SingleObjectDto<ChartsDto>> charts(
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            dashboardService.chartsAvecScope(principal.getRole(), principal.getDepartementId())));
  }
}
