package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.DeltaJourDto;
import com.sirh.model.dto.PlanningAffecterRequest;
import com.sirh.model.dto.PlanningDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.PlanningService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plannings")
@RequiredArgsConstructor
public class PlanningController {

  private final PlanningService planningService;

  @PostMapping("/affecter")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<PlanningDto>> affecter(
      @Valid @RequestBody PlanningAffecterRequest request) {
    PlanningDto planning =
        planningService.affecter(
            request.employeId(),
            request.shiftId(),
            request.dateDebut(),
            request.dateFin(),
            request.statut(),
            request.commentaire());
    return ResponseEntity.status(201).body(SingleObjectDto.of(planning));
  }

  @GetMapping("/departement/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<ListingPageDto<PlanningDto>> parDepartement(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            planningService.findByDepartementAvecScope(
                id, dateDebut, dateFin, principal.getRole(), principal.getDepartementId())));
  }

  @GetMapping("/delta")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<ListingPageDto<DeltaJourDto>> delta(
      @RequestParam(required = false) UUID employeId,
      @RequestParam(required = false) Long departementId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            planningService.deltaAvecScope(
                employeId,
                departementId,
                dateDebut,
                dateFin,
                principal.getRole(),
                principal.getDepartementId())));
  }
}
