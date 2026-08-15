package com.sirh.controller;

import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.ContratDto;
import com.sirh.model.dto.ContratRequest;
import com.sirh.service.ContratService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employes/{employeId}/contrats")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class ContratController {

    private final ContratService contratService;

    public ContratController(ContratService contratService) {
        this.contratService = contratService;
    }

    @GetMapping
    public ResponseEntity<List<ContratDto>> list(@PathVariable UUID employeId) {
        List<ContratDto> dtos = contratService.findByEmploye(employeId).stream()
                .map(ContratDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ContratDto> create(@PathVariable UUID employeId,
                                              @Valid @RequestBody ContratRequest request,
                                              @AuthenticationPrincipal SirhUserDetails principal) {
        var contrat = contratService.create(employeId, request, principal.getUserId());
        return ResponseEntity.status(201).body(ContratDto.from(contrat));
    }
}