package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ResetPasswordRequest;
import com.sirh.model.dto.UtilisateurCreateRequest;
import com.sirh.model.dto.UtilisateurDto;
import com.sirh.model.dto.UtilisateurUpdateRequest;
import com.sirh.service.UtilisateurService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utilisateurs")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class UtilisateurController {

  private final UtilisateurService utilisateurService;

  @GetMapping
  public ResponseEntity<ListingPageDto<UtilisateurDto>> list() {
    return ResponseEntity.ok(ListingPageDto.of(utilisateurService.findAll()));
  }

  @GetMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> getUtilisateurByReference(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(utilisateurService.getUtilisateurByReference(reference)));
  }

  @PostMapping
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> create(
      @Valid @RequestBody UtilisateurCreateRequest request) {
    String passwordHash = utilisateurService.hashPassword(request.password());
    var utilisateur =
        utilisateurService.create(
            request.employeId(), request.username(), passwordHash, request.role());
    return ResponseEntity.status(201).body(SingleObjectDto.of(utilisateur));
  }

  @PutMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> updateUtilisateurByReference(
      @PathVariable UUID reference, @Valid @RequestBody UtilisateurUpdateRequest request) {
    var utilisateur =
        utilisateurService.updateUtilisateurByReference(
            reference, request.role(), request.compteActif());
    return ResponseEntity.ok(SingleObjectDto.of(utilisateur));
  }

  @PutMapping("/{reference}/reset-password")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID reference, @Valid @RequestBody ResetPasswordRequest request) {
    String passwordHash = utilisateurService.hashPassword(request.newPassword());
    utilisateurService.resetPassword(reference, passwordHash);
    return ResponseEntity.noContent().build();
  }
}
