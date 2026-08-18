package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.JourFerieDto;
import com.sirh.model.dto.JourFerieRequest;
import com.sirh.service.CalendrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Calendrier",
    description = "Gestion des jours fériés et fermetures collectives (cahier-technique.md §4.3)")
@RestController
@RequestMapping("/calendrier/jours-feries")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
@RequiredArgsConstructor
public class CalendrierController {

  private final CalendrierService calendrierService;

  @Operation(summary = "Lister les jours fériés d'une période")
  @GetMapping
  public ResponseEntity<ListingPageDto<JourFerieDto>> list(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
    return ResponseEntity.ok(
        ListingPageDto.of(calendrierService.findByPeriode(dateDebut, dateFin)));
  }

  @Operation(
      summary = "Créer un jour férié",
      description =
          "Si `type = FERIE` chevauche une indisponibilité de congé `CONGE_PAYE` déjà `VALIDE`"
              + " d'un employé, re-crédit automatique du solde (`historique_soldes`, motif"
              + " `RECREDIT_FERIE`). Pas de re-crédit pour `FERMETURE_COLLECTIVE` ni les autres"
              + " types d'indisponibilité (ex. `MALADIE`).")
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<JourFerieDto>> createJourFerie(
      @Valid @RequestBody JourFerieRequest request) {
    JourFerieDto jourFerie =
        calendrierService.creerJourFerie(
            request.date(),
            request.libelle(),
            request.type(),
            request.national(),
            request.region());
    return ResponseEntity.status(201).body(SingleObjectDto.of(jourFerie));
  }

  @Operation(
      summary = "Importer des jours fériés en masse",
      description =
          "Import atomique : toute ligne en échec (ex. date en doublon) annule l'import entier,"
              + " aucune insertion partielle.")
  @PostMapping("/import")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<JourFerieDto>> importer(
      @RequestBody List<JourFerieRequest> lignes) {
    List<JourFerieDto> importes = calendrierService.importer(lignes);
    return ResponseEntity.status(201).body(ListingPageDto.of(importes));
  }

  @Operation(
      summary = "Modifier un jour férié",
      description =
          "Ne déclenche jamais le re-crédit de solde — celui-ci n'a lieu qu'à la création, pour"
              + " éviter un double crédit sur une simple correction de libellé.")
  @PutMapping("/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<JourFerieDto>> updateJourFerieByReference(
      @PathVariable UUID reference, @Valid @RequestBody JourFerieRequest request) {
    JourFerieDto jourFerie =
        calendrierService.modifierJourFerieByReference(
            reference,
            request.date(),
            request.libelle(),
            request.type(),
            request.national(),
            request.region());
    return ResponseEntity.ok(SingleObjectDto.of(jourFerie));
  }

  @Operation(summary = "Supprimer un jour férié")
  @DeleteMapping("/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<Void> deleteJourFerieByReference(@PathVariable UUID reference) {
    calendrierService.supprimerJourFerieByReference(reference);
    return ResponseEntity.noContent().build();
  }
}
