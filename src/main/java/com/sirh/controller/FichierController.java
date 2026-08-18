package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDemandeUploadRequest;
import com.sirh.model.dto.FichierDownloadUrlDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.dto.FichierUploadUrlDto;
import com.sirh.model.enums.EntiteType;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.FichierService;
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
    name = "Fichiers",
    description =
        "Upload/téléchargement via URLs pré-signées (S3-compatible, aucun binaire ne transite par"
            + " le backend) et suppression de fichiers rattachés à une entité métier")
@RestController
@RequestMapping("/fichiers")
@RequiredArgsConstructor
public class FichierController {

  private final FichierService fichierService;

  @Operation(
      summary = "Demander une URL pré-signée d'upload",
      description =
          "Tout rôle authentifié — rattaché en option à une entité métier (employé, contrat, ...)."
              + " Le client doit ensuite PUT le fichier directement sur `upload_url`, puis appeler"
              + " `POST /fichiers/{reference}/confirmer`.")
  @PostMapping("/demande-upload")
  public ResponseEntity<SingleObjectDto<FichierUploadUrlDto>> demanderUpload(
      @Valid @RequestBody FichierDemandeUploadRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    FichierUploadUrlDto dto = fichierService.demanderUpload(request, principal.getUserId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(dto));
  }

  @Operation(
      summary = "Confirmer un upload",
      description =
          "Tout rôle authentifié. Vérifie côté stockage objet que le fichier a bien été déposé sur"
              + " `upload_url` (statut passe à DISPONIBLE ou ECHEC).")
  @PostMapping("/{reference}/confirmer")
  public ResponseEntity<SingleObjectDto<FichierDto>> confirmerUpload(@PathVariable UUID reference) {
    return ResponseEntity.ok(SingleObjectDto.of(fichierService.confirmerUpload(reference)));
  }

  @Operation(
      summary = "Obtenir une URL pré-signée de téléchargement",
      description = "Tout rôle authentifié. Le fichier doit être au statut DISPONIBLE.")
  @GetMapping("/{reference}/download-url")
  public ResponseEntity<SingleObjectDto<FichierDownloadUrlDto>> genererUrlTelechargement(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(fichierService.genererUrlTelechargement(reference)));
  }

  @Operation(
      summary = "Lister les fichiers liés à une entité",
      description = "Tout rôle authentifié.")
  @GetMapping("/entite/{type}/{id}")
  public ResponseEntity<ListingPageDto<FichierDto>> listByEntite(
      @PathVariable("type") EntiteType type, @PathVariable("id") Long id) {
    return ResponseEntity.ok(ListingPageDto.of(fichierService.listByEntite(type, id)));
  }

  @Operation(
      summary = "Supprimer un fichier",
      description =
          "Suppression logique (`deletedAt`, BaseEntity) — l'objet stocké n'est pas physiquement"
              + " effacé (bucket documents RH versionné).")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @DeleteMapping("/{reference}")
  public ResponseEntity<Void> deleteFichierByReference(@PathVariable UUID reference) {
    fichierService.deleteFichierByReference(reference);
    return ResponseEntity.noContent().build();
  }
}
