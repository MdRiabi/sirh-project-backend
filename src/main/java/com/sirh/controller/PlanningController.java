package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.DeltaJourDto;
import com.sirh.model.dto.PlanningAffecterRequest;
import com.sirh.model.dto.PlanningDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
    name = "Plannings",
    description =
        "Affectation de créneaux aux employés et consultation des plannings (cahier-technique.md"
            + " §4.4)")
@RestController
@RequestMapping("/plannings")
@RequiredArgsConstructor
public class PlanningController {

  private final PlanningService planningService;

  @Operation(
      summary = "Affecter un shift à un employé",
      description =
          "Contrôle automatique et bloquant du repos légal de 11h consécutives entre deux shifts"
              + " : l'affectation est refusée si elle viole ce repos ou si elle chevauche une"
              + " indisponibilité de congé déjà `VALIDE` (cahier-technique.md §4.4/§5).")
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

  @Operation(summary = "Consulter le planning d'un département")
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

  @Operation(
      summary = "Comparer planning théorique et pointages réels (delta)",
      description =
          "Croisement synchrone planning théorique / pointage réel, jour par jour. Filtre"
              + " obligatoire et mutuellement exclusif : exactement un des deux paramètres"
              + " `employeId` OU `departementId`. Pour MANAGER, restreint à son propre"
              + " département.")
  @GetMapping("/delta")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<ListingPageDto<DeltaJourDto>> delta(
      @Parameter(
              description = "Mutuellement exclusif avec `departementId` — l'un des deux est requis")
          @RequestParam(required = false)
          UUID employeId,
      @Parameter(description = "Mutuellement exclusif avec `employeId` — l'un des deux est requis")
          @RequestParam(required = false)
          Long departementId,
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
