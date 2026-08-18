package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ResetPasswordRequest;
import com.sirh.model.dto.UtilisateurCreateRequest;
import com.sirh.model.dto.UtilisateurDto;
import com.sirh.model.dto.UtilisateurUpdateRequest;
import com.sirh.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Utilisateurs",
    description = "Comptes utilisateurs, réservé RH/SUPERUSER (cahier-technique.md §4.1)")
@RestController
@RequestMapping("/utilisateurs")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class UtilisateurController {

  private final UtilisateurService utilisateurService;

  @Operation(summary = "Lister les comptes utilisateurs")
  @GetMapping
  public ResponseEntity<ListingPageDto<UtilisateurDto>> list() {
    return ResponseEntity.ok(ListingPageDto.of(utilisateurService.findAll()));
  }

  @Operation(summary = "Consulter un compte utilisateur")
  @GetMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> getUtilisateurByReference(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(utilisateurService.getUtilisateurByReference(reference)));
  }

  @Operation(summary = "Créer un compte utilisateur")
  @PostMapping
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> createUtilisateur(
      @Valid @RequestBody UtilisateurCreateRequest request) {
    String passwordHash = utilisateurService.hashPassword(request.password());
    var utilisateur =
        utilisateurService.createUtilisateur(
            request.employeId(), request.username(), passwordHash, request.role());
    return ResponseEntity.status(201).body(SingleObjectDto.of(utilisateur));
  }

  @Operation(summary = "Modifier le rôle ou le statut d'un compte utilisateur")
  @PutMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<UtilisateurDto>> updateUtilisateurByReference(
      @PathVariable UUID reference, @Valid @RequestBody UtilisateurUpdateRequest request) {
    var utilisateur =
        utilisateurService.updateUtilisateurByReference(
            reference, request.role(), request.compteActif());
    return ResponseEntity.ok(SingleObjectDto.of(utilisateur));
  }

  @Operation(summary = "Réinitialiser le mot de passe d'un compte utilisateur")
  @PutMapping("/{reference}/reset-password")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID reference, @Valid @RequestBody ResetPasswordRequest request) {
    String passwordHash = utilisateurService.hashPassword(request.newPassword());
    utilisateurService.resetPassword(reference, passwordHash);
    return ResponseEntity.noContent().build();
  }
}
