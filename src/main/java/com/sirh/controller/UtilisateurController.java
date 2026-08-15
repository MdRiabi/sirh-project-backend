package com.sirh.controller;

import com.sirh.model.dto.ResetPasswordRequest;
import com.sirh.model.dto.UtilisateurCreateRequest;
import com.sirh.model.dto.UtilisateurDto;
import com.sirh.model.dto.UtilisateurUpdateRequest;
import com.sirh.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utilisateurs")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurDto>> list() {
        List<UtilisateurDto> dtos = utilisateurService.findAll().stream().map(UtilisateurDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(UtilisateurDto.from(utilisateurService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<UtilisateurDto> create(@Valid @RequestBody UtilisateurCreateRequest request) {
        String passwordHash = utilisateurService.hashPassword(request.password());
        var utilisateur = utilisateurService.create(request.employeId(), request.username(), passwordHash, request.role());
        return ResponseEntity.status(201).body(UtilisateurDto.from(utilisateur));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurDto> update(@PathVariable Long id, @Valid @RequestBody UtilisateurUpdateRequest request) {
        var utilisateur = utilisateurService.update(id, request.role(), request.compteActif());
        return ResponseEntity.ok(UtilisateurDto.from(utilisateur));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        String passwordHash = utilisateurService.hashPassword(request.newPassword());
        utilisateurService.resetPassword(id, passwordHash);
        return ResponseEntity.noContent().build();
    }
}