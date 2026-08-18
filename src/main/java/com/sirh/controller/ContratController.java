package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ContratDto;
import com.sirh.model.dto.ContratRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.ContratService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Contrats",
    description = "Contrats de travail rattachés à un employé (Vue 360°, cahier-technique.md §3.3)")
@RestController
@RequestMapping("/employes/{employeId}/contrats")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class ContratController {

  private final ContratService contratService;

  @Operation(summary = "Lister les contrats d'un employé")
  @GetMapping
  public ResponseEntity<ListingPageDto<ContratDto>> list(@PathVariable UUID employeId) {
    return ResponseEntity.ok(ListingPageDto.of(contratService.findByEmploye(employeId)));
  }

  @Operation(summary = "Créer un contrat pour un employé")
  @PostMapping
  public ResponseEntity<SingleObjectDto<ContratDto>> create(
      @PathVariable UUID employeId,
      @Valid @RequestBody ContratRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var contrat = contratService.createContrat(employeId, request, principal.getUserId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(contrat));
  }
}
