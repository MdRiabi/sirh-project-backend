package com.sirh.controller;

import com.sirh.model.dto.EmployeDto;
import com.sirh.model.dto.EmployeRequest;
import com.sirh.model.entity.Employe;
import com.sirh.service.EmployeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/employes")
public class EmployeController {

    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    // cahier-technique.md §4.2 : Employés/Départements en lecture restent ouverts
    // à MANAGER (hérité du v1), au-delà de RH/SUPERUSER — nécessaire notamment
    // pour la vue Planning en lecture seule de son équipe (Lot 3).
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    @GetMapping
    public ResponseEntity<Page<EmployeDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) Boolean actif,
            Pageable pageable) {
        Page<EmployeDto> page = employeService.search(search, departementId, actif, pageable).map(EmployeDto::from);
        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeDto.from(employeService.findById(id)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    @PostMapping
    public ResponseEntity<EmployeDto> create(@Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.status(201).body(EmployeDto.from(employeService.create(request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeDto> update(@PathVariable UUID id, @Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.ok(EmployeDto.from(employeService.update(id, request)));
    }

    // Affichage complet du matricule_cnss réservé à une action explicite RH/SuperUser
    // (ARCHITECTURE.md §10) — jamais renvoyé automatiquement dans la liste/fiche.
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    @GetMapping("/{id}/matricule-cnss")
    public ResponseEntity<Map<String, String>> revealMatriculeCnss(@PathVariable UUID id) {
        Employe employe = employeService.findById(id);
        return ResponseEntity.ok(Map.of("matriculeCnss", employe.getMatriculeCnss() != null ? employe.getMatriculeCnss() : ""));
    }
}