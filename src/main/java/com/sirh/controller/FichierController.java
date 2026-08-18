package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.FichierService;
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

@RestController
@RequestMapping("/fichiers")
@RequiredArgsConstructor
public class FichierController {

  private final FichierService fichierService;

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

  @GetMapping("/entite/{type}/{id}")
  public ResponseEntity<ListingPageDto<FichierDto>> listByEntite(
      @PathVariable("type") EntiteType type, @PathVariable("id") Long id) {
    return ResponseEntity.ok(ListingPageDto.of(fichierService.listByEntite(type, id)));
  }

  @DeleteMapping("/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<Void> deleteFichierByReference(@PathVariable UUID reference) {
    fichierService.deleteFichierByReference(reference);
    return ResponseEntity.noContent().build();
  }
}
