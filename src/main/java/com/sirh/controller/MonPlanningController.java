package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.model.dto.PlanningDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Consultation stricte de son propre planning (cahier-technique.md §4.4) : le
// filtre employe_id vient exclusivement du JWT, jamais d'un paramètre client.
@Tag(
    name = "Mon Planning",
    description = "Consultation par l'employé de son propre planning (lecture seule)")
@RestController
@RequestMapping("/mon-planning")
@PreAuthorize("hasAuthority('ROLE_EMPLOYE')")
@RequiredArgsConstructor
public class MonPlanningController {

  private final PlanningService planningService;

  @Operation(
      summary = "Consulter mon planning",
      description =
          "Vue strictement en lecture seule, filtrée côté serveur sur `employe_id = JWT.employeId`"
              + " — l'employé ne peut jamais consulter le planning d'un autre.")
  @GetMapping
  public ResponseEntity<ListingPageDto<PlanningDto>> get(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            planningService.findByEmploye(principal.getEmployeId(), dateDebut, dateFin)));
  }
}
