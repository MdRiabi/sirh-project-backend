package com.sirh.personnel.controller;

import com.sirh.personnel.dto.EmployeDto;
import com.sirh.personnel.dto.EmployeRequest;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.service.EmployeService;
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
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class EmployeController {

    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    @GetMapping
    public ResponseEntity<Page<EmployeDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) Boolean actif,
            Pageable pageable) {
        Page<EmployeDto> page = employeService.search(search, departementId, actif, pageable).map(EmployeDto::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeDto.from(employeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<EmployeDto> create(@Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.status(201).body(EmployeDto.from(employeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeDto> update(@PathVariable UUID id, @Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.ok(EmployeDto.from(employeService.update(id, request)));
    }

    // Affichage complet du matricule_cnss réservé à une action explicite RH/SuperUser
    // (ARCHITECTURE.md §10) — jamais renvoyé automatiquement dans la liste/fiche.
    @GetMapping("/{id}/matricule-cnss")
    public ResponseEntity<Map<String, String>> revealMatriculeCnss(@PathVariable UUID id) {
        Employe employe = employeService.findById(id);
        return ResponseEntity.ok(Map.of("matriculeCnss", employe.getMatriculeCnss() != null ? employe.getMatriculeCnss() : ""));
    }
}