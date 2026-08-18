package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.FichierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(
    name = "Fichiers",
    description = "Upload, téléchargement et suppression de fichiers rattachés à une entité métier")
@RestController
@RequestMapping("/fichiers")
@RequiredArgsConstructor
public class FichierController {

  private final FichierService fichierService;

  @Operation(
      summary = "Uploader un fichier",
      description =
          "Tout rôle authentifié — rattaché en option à une entité métier (employé, contrat, ...).")
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SingleObjectDto<FichierDto>> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("categorie") FichierCategorie categorie,
      @RequestParam(value = "entiteType", required = false) EntiteType entiteType,
      @RequestParam(value = "entiteId", required = false) Long entiteId,
      @AuthenticationPrincipal SirhUserDetails principal) {
    FichierDto fichier =
        fichierService.upload(file, categorie, entiteType, entiteId, principal.getUserId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(fichier));
  }

  @Operation(summary = "Télécharger un fichier", description = "Tout rôle authentifié.")
  @GetMapping("/{reference}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID reference) {
    Fichier fichier = fichierService.getFichierByReference(reference);
    Resource resource = fichierService.loadAsResource(fichier);
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(fichier.getNomOriginal()).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(resource);
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
          "Suppression logique (`deletedAt`, BaseEntity) — le fichier stocké n'est pas physiquement"
              + " effacé.")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  @DeleteMapping("/{reference}")
  public ResponseEntity<Void> deleteFichierByReference(@PathVariable UUID reference) {
    fichierService.deleteFichierByReference(reference);
    return ResponseEntity.noContent().build();
  }
}
