package com.sirh.planning.controller;

import com.sirh.planning.dto.ShiftDto;
import com.sirh.planning.dto.ShiftRequest;
import com.sirh.planning.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shifts")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping
    public ResponseEntity<List<ShiftDto>> list() {
        return ResponseEntity.ok(shiftService.findAll().stream().map(ShiftDto::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ShiftDto> create(@Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.status(201).body(ShiftDto.from(shiftService.create(request)));
    }
}