package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.EmployeDto;
import com.sirh.model.dto.EmployeRequest;
import com.sirh.service.EmployeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Employés",
    description =
        "CRUD employés et consultation du matricule CNSS masqué (cahier-technique.md §4.2)")
@RestController
@RequestMapping("/employes")
@RequiredArgsConstructor
public class EmployeController {

  private final EmployeService employeService;

  // cahier-technique.md §4.2 : Employés/Départements en lecture restent ouverts
  // à MANAGER (hérité du v1), au-delà de RH/SUPERUSER — nécessaire notamment
  // pour la vue Planning en lecture seule de son équipe (Lot 3).
  @Operation(
      summary = "Lister les employés",
      description =
          "Recherche paginée avec filtres optionnels combinables. `matricule_cnss` dans la"
              + " réponse est masqué par défaut (cf. `GET /{id}/matricule-cnss` pour le révéler).")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  @GetMapping
  public ResponseEntity<ListingPageDto<EmployeDto>> list(
      @Parameter(description = "Recherche libre sur nom/prénom/matricule interne")
          @RequestParam(required = false)
          String search,
      @RequestParam(required = false) Long departementId,
      @RequestParam(required = false) Boolean actif,
      Pageable pageable) {
    Page<EmployeDto> page = employeService.search(search, departementId, actif, pageable);
    return ResponseEntity.ok(ListingPageDto.of(page));
  }

  @Operation(summary = "Consulter la fiche d'un employé")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  @GetMapping("/{id}")
  public ResponseEntity<SingleObjectDto<EmployeDto>> get(@PathVariable UUID id) {
    return ResponseEntity.ok(SingleObjectDto.of(employeService.getEmployeDto(id)));
  }

  @Operation(summary = "Créer un employé")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @PostMapping
  public ResponseEntity<SingleObjectDto<EmployeDto>> create(
      @Valid @RequestBody EmployeRequest request) {
    return ResponseEntity.status(201)
        .body(SingleObjectDto.of(employeService.createEmploye(request)));
  }

  @Operation(summary = "Mettre à jour la fiche d'un employé")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @PutMapping("/{id}")
  public ResponseEntity<SingleObjectDto<EmployeDto>> update(
      @PathVariable UUID id, @Valid @RequestBody EmployeRequest request) {
    return ResponseEntity.ok(SingleObjectDto.of(employeService.updateEmploye(id, request)));
  }

  // Affichage complet du matricule_cnss réservé à une action explicite RH/SuperUser
  // (ARCHITECTURE.md §10) — jamais renvoyé automatiquement dans la liste/fiche.
  @Operation(
      summary = "Révéler le matricule CNSS complet",
      description =
          "Donnée sensible masquée partout ailleurs (ARCHITECTURE.md §10) — action explicite et"
              + " auditée (`@Audited`) pour obtenir la valeur en clair.")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @GetMapping("/{id}/matricule-cnss")
  public ResponseEntity<SingleObjectDto<Map<String, String>>> revealMatriculeCnss(
      @PathVariable UUID id) {
    return ResponseEntity.ok(
        SingleObjectDto.of(Map.of("matricule_cnss", employeService.revealMatriculeCnss(id))));
  }
}
