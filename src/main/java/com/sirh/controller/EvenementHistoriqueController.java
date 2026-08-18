package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.EvenementDto;
import com.sirh.model.dto.EvenementRequest;
import com.sirh.model.enums.EvenementCategorie;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.EvenementHistoriqueService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employes/{employeId}")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class EvenementHistoriqueController {

  private final EvenementHistoriqueService evenementHistoriqueService;

  @GetMapping("/timeline")
  public ResponseEntity<ListingPageDto<EvenementDto>> timeline(
      @PathVariable UUID employeId,
      @RequestParam(required = false) EvenementCategorie categorie,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateDebut,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateFin) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            evenementHistoriqueService.findTimeline(employeId, categorie, dateDebut, dateFin)));
  }

  @PostMapping("/evenements")
  public ResponseEntity<SingleObjectDto<EvenementDto>> createEvenement(
      @PathVariable UUID employeId,
      @Valid @RequestBody EvenementRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var evenement =
        evenementHistoriqueService.createManualEntry(employeId, request, principal.getUserId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(evenement));
  }
}
