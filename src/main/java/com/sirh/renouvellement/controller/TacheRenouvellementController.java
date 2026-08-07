package com.sirh.renouvellement.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.renouvellement.dto.TacheRenouvellementDto;
import com.sirh.renouvellement.dto.TacheRenouvellementTraiterRequest;
import com.sirh.renouvellement.entity.TacheRenouvellementStatut;
import com.sirh.renouvellement.service.RenouvellementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}")
    public ResponseEntity<TacheRenouvellementDto> traiter(@PathVariable Long id,
                                                            @Valid @RequestBody TacheRenouvellementTraiterRequest request,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(renouvellementService.traiter(id, request.statut(), request.dateFin(), principal.getUserId()));
    }
}
