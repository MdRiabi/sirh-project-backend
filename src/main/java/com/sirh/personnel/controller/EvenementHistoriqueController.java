package com.sirh.personnel.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.personnel.dto.EvenementDto;
import com.sirh.personnel.dto.EvenementRequest;
import com.sirh.personnel.entity.EvenementCategorie;
import com.sirh.personnel.service.EvenementHistoriqueService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employes/{employeId}")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class EvenementHistoriqueController {

    private final EvenementHistoriqueService evenementHistoriqueService;

    public EvenementHistoriqueController(EvenementHistoriqueService evenementHistoriqueService) {
        this.evenementHistoriqueService = evenementHistoriqueService;
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<EvenementDto>> timeline(
            @PathVariable UUID employeId,
            @RequestParam(required = false) EvenementCategorie categorie,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        List<EvenementDto> dtos = evenementHistoriqueService.findTimeline(employeId, categorie, dateDebut, dateFin)
                .stream().map(EvenementDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/evenements")
    public ResponseEntity<EvenementDto> createEvenement(
            @PathVariable UUID employeId,
            @Valid @RequestBody EvenementRequest request,
            @AuthenticationPrincipal SirhUserDetails principal) {
        var evenement = evenementHistoriqueService.createManualEntry(employeId, request, principal.getUserId());
        return ResponseEntity.status(201).body(EvenementDto.from(evenement));
    }
}