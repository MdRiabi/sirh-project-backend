package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.JourFerieDto;
import com.sirh.model.dto.JourFerieRequest;
import com.sirh.model.entity.JourFerie;
import com.sirh.service.CalendrierService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/calendrier/jours-feries")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
public class CalendrierController {

    private final CalendrierService calendrierService;
    private final Validator validator;

    public CalendrierController(CalendrierService calendrierService, Validator validator) {
        this.calendrierService = calendrierService;
        this.validator = validator;
    }

    @GetMapping
    public ResponseEntity<ListingPageDto<JourFerieDto>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(ListingPageDto.of(calendrierService.findByPeriode(dateDebut, dateFin)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<SingleObjectDto<JourFerieDto>> create(@Valid @RequestBody JourFerieRequest request) {
        JourFerieDto jourFerie = calendrierService.creer(
                request.date(), request.libelle(), request.type(), request.national(), request.region());
        return ResponseEntity.status(201).body(SingleObjectDto.of(jourFerie));
    }

    // @Valid ne cascade pas sur les éléments d'un List<T> passé directement en
    // @RequestBody (limitation Bean Validation / Spring MVC) : chaque ligne est
    // donc validée manuellement ici, sinon une ligne invalide (date/libelle/type
    // null) échapperait au contrôle DTO et ne serait rejetée que par une
    // contrainte NOT NULL en base, avec un statut 409 trompeur au lieu de 400.
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ListingPageDto<JourFerieDto>> importer(@RequestBody List<JourFerieRequest> lignes) {
        for (JourFerieRequest ligne : lignes) {
            Set<ConstraintViolation<JourFerieRequest>> violations = validator.validate(ligne);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                        "Ligne d'import invalide : " + violations.iterator().next().getMessage());
            }
        }
        List<JourFerie> aImporter = lignes.stream()
                .map(r -> JourFerie.builder()
                        .date(r.date()).libelle(r.libelle()).type(r.type())
                        .national(r.national() != null ? r.national() : true)
                        .region(r.region())
                        .build())
                .toList();
        List<JourFerieDto> importes = calendrierService.importer(aImporter);
        return ResponseEntity.status(201).body(ListingPageDto.of(importes));
    }

    @PutMapping("/{reference}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<SingleObjectDto<JourFerieDto>> updateJourFerieByReference(@PathVariable UUID reference, @Valid @RequestBody JourFerieRequest request) {
        JourFerieDto jourFerie = calendrierService.modifierJourFerieByReference(
                reference, request.date(), request.libelle(), request.type(), request.national(), request.region());
        return ResponseEntity.ok(SingleObjectDto.of(jourFerie));
    }

    @DeleteMapping("/{reference}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<Void> deleteJourFerieByReference(@PathVariable UUID reference) {
        calendrierService.supprimerJourFerieByReference(reference);
        return ResponseEntity.noContent().build();
    }
}
