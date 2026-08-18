package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ParametreGlobalDto;
import com.sirh.model.dto.ParametreGlobalUpdateRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.ParametreGlobalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Paramétrage global RH (cahier-technique.md §3.24/§4.11) : valeurs modifiables
// à chaud (ex. taux d'incrémentation mensuelle des congés, Lot 5), jamais
// codées en dur. RH/SUPERUSER uniquement — une valeur mal ajustée impacte
// tous les employés.
@RestController
@RequestMapping("/parametres-globaux")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class ParametreGlobalController {

  private final ParametreGlobalService parametreGlobalService;

  @GetMapping
  public ResponseEntity<ListingPageDto<ParametreGlobalDto>> list() {
    return ResponseEntity.ok(ListingPageDto.of(parametreGlobalService.findAll()));
  }

  @GetMapping("/{cle}")
  public ResponseEntity<SingleObjectDto<ParametreGlobalDto>> get(@PathVariable String cle) {
    return ResponseEntity.ok(SingleObjectDto.of(parametreGlobalService.getParametreGlobalDto(cle)));
  }

  @PutMapping("/{cle}")
  public ResponseEntity<SingleObjectDto<ParametreGlobalDto>> update(
      @PathVariable String cle,
      @Valid @RequestBody ParametreGlobalUpdateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            parametreGlobalService.modifierValeur(
                cle, request.valeur(), principal.getEmployeId())));
  }
}
