package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.JourFerieDto;
import com.sirh.model.dto.JourFerieRequest;
import com.sirh.service.CalendrierService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/calendrier/jours-feries")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
@RequiredArgsConstructor
public class CalendrierController {

  private final CalendrierService calendrierService;

  @GetMapping
  public ResponseEntity<ListingPageDto<JourFerieDto>> list(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
    return ResponseEntity.ok(
        ListingPageDto.of(calendrierService.findByPeriode(dateDebut, dateFin)));
  }

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

  @PostMapping("/import")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<JourFerieDto>> importer(
      @RequestBody List<JourFerieRequest> lignes) {
    List<JourFerieDto> importes = calendrierService.importer(lignes);
    return ResponseEntity.status(201).body(ListingPageDto.of(importes));
  }

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

  @DeleteMapping("/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<Void> deleteJourFerieByReference(@PathVariable UUID reference) {
    calendrierService.supprimerJourFerieByReference(reference);
    return ResponseEntity.noContent().build();
  }
}
