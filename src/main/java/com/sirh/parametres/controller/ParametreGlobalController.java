package com.sirh.parametres.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.parametres.dto.ParametreGlobalDto;
import com.sirh.parametres.dto.ParametreGlobalUpdateRequest;
import com.sirh.parametres.service.ParametreGlobalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Paramétrage global RH (cahier-technique.md §3.24/§4.11) : valeurs modifiables
// à chaud (ex. taux d'incrémentation mensuelle des congés, Lot 5), jamais
// codées en dur. RH/SUPERUSER uniquement — une valeur mal ajustée impacte
// tous les employés.
@RestController
@RequestMapping("/parametres-globaux")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class ParametreGlobalController {

    private final ParametreGlobalService parametreGlobalService;

    public ParametreGlobalController(ParametreGlobalService parametreGlobalService) {
        this.parametreGlobalService = parametreGlobalService;
    }

    @GetMapping
    public ResponseEntity<List<ParametreGlobalDto>> list() {
        return ResponseEntity.ok(parametreGlobalService.findAll().stream().map(ParametreGlobalDto::from).toList());
    }

    @GetMapping("/{cle}")
    public ResponseEntity<ParametreGlobalDto> get(@PathVariable String cle) {
        return ResponseEntity.ok(ParametreGlobalDto.from(parametreGlobalService.findByCle(cle)));
    }

    @PutMapping("/{cle}")
    public ResponseEntity<ParametreGlobalDto> update(@PathVariable String cle,
                                                       @Valid @RequestBody ParametreGlobalUpdateRequest request,
                                                       @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(ParametreGlobalDto.from(
                parametreGlobalService.modifierValeur(cle, request.valeur(), principal.getEmployeId())));
    }
}
