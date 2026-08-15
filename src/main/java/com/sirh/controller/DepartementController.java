package com.sirh.controller;

import com.sirh.model.dto.DepartementDto;
import com.sirh.model.dto.DepartementRequest;
import com.sirh.service.DepartementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departements")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class DepartementController {

    private final DepartementService departementService;

    public DepartementController(DepartementService departementService) {
        this.departementService = departementService;
    }

    @GetMapping
    public ResponseEntity<List<DepartementDto>> list() {
        List<DepartementDto> dtos = departementService.findAll().stream()
                .map(DepartementDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartementDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(DepartementDto.from(departementService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<DepartementDto> create(@Valid @RequestBody DepartementRequest request) {
        return ResponseEntity.status(201).body(DepartementDto.from(departementService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartementDto> update(@PathVariable Long id, @Valid @RequestBody DepartementRequest request) {
        return ResponseEntity.ok(DepartementDto.from(departementService.update(id, request)));
    }
}