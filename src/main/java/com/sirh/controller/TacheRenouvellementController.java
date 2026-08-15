package com.sirh.controller;

import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.TacheRenouvellementDto;
import com.sirh.model.dto.TacheRenouvellementTraiterRequest;
import com.sirh.model.enums.TacheRenouvellementStatut;
import com.sirh.service.RenouvellementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/taches-renouvellement")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class TacheRenouvellementController {

    private final RenouvellementService renouvellementService;

    public TacheRenouvellementController(RenouvellementService renouvellementService) {
        this.renouvellementService = renouvellementService;
    }

    @GetMapping
    public ResponseEntity<List<TacheRenouvellementDto>> list(@RequestParam(required = false) TacheRenouvellementStatut statut) {
        return ResponseEntity.ok(renouvellementService.search(statut));
    }

    @PutMapping("/{reference}")
    public ResponseEntity<TacheRenouvellementDto> traiterTacheRenouvellementByReference(@PathVariable UUID reference,
                                                            @Valid @RequestBody TacheRenouvellementTraiterRequest request,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(renouvellementService.traiterTacheRenouvellementByReference(reference, request.statut(), request.dateFin(), principal.getUserId()));
    }
}
