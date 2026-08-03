package com.sirh.calendrier.controller;

import com.sirh.calendrier.dto.JourFerieDto;
import com.sirh.calendrier.dto.JourFerieRequest;
import com.sirh.calendrier.entity.JourFerie;
import com.sirh.calendrier.service.CalendrierService;
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
    public ResponseEntity<List<JourFerieDto>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(calendrierService.findByPeriode(dateDebut, dateFin).stream()
                .map(JourFerieDto::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<JourFerieDto> create(@Valid @RequestBody JourFerieRequest request) {
        JourFerie jourFerie = calendrierService.creer(
                request.date(), request.libelle(), request.type(), request.national(), request.region());
        return ResponseEntity.status(201).body(JourFerieDto.from(jourFerie));
    }

    // @Valid ne cascade pas sur les éléments d'un List<T> passé directement en
    // @RequestBody (limitation Bean Validation / Spring MVC) : chaque ligne est
    // donc validée manuellement ici, sinon une ligne invalide (date/libelle/type
    // null) échapperait au contrôle DTO et ne serait rejetée que par une
    // contrainte NOT NULL en base, avec un statut 409 trompeur au lieu de 400.
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<JourFerieDto>> importer(@RequestBody List<JourFerieRequest> lignes) {
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
        List<JourFerieDto> importes = calendrierService.importer(aImporter).stream().map(JourFerieDto::from).toList();
        return ResponseEntity.status(201).body(importes);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<JourFerieDto> update(@PathVariable Long id, @Valid @RequestBody JourFerieRequest request) {
        JourFerie jourFerie = calendrierService.modifier(
                id, request.date(), request.libelle(), request.type(), request.national(), request.region());
        return ResponseEntity.ok(JourFerieDto.from(jourFerie));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendrierService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
