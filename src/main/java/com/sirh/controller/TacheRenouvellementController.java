package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.TacheRenouvellementDto;
import com.sirh.model.dto.TacheRenouvellementTraiterRequest;
import com.sirh.model.enums.TacheRenouvellementStatut;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.RenouvellementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/taches-renouvellement")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class TacheRenouvellementController {

  private final RenouvellementService renouvellementService;

  @GetMapping
  public ResponseEntity<ListingPageDto<TacheRenouvellementDto>> list(
      @RequestParam(required = false) TacheRenouvellementStatut statut) {
    return ResponseEntity.ok(ListingPageDto.of(renouvellementService.search(statut)));
  }

  @PutMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<TacheRenouvellementDto>>
      traiterTacheRenouvellementByReference(
          @PathVariable UUID reference,
          @Valid @RequestBody TacheRenouvellementTraiterRequest request,
          @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            renouvellementService.traiterTacheRenouvellementByReference(
                reference, request.statut(), request.dateFin(), principal.getUserId())));
  }
}
