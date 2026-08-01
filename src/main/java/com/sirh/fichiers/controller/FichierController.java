package com.sirh.fichiers.controller;

import com.sirh.auth.security.SirhUserDetails;
import com.sirh.fichiers.dto.FichierDto;
import com.sirh.fichiers.entity.EntiteType;
import com.sirh.fichiers.entity.Fichier;
import com.sirh.fichiers.entity.FichierCategorie;
import com.sirh.fichiers.service.FichierService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/fichiers")
public class FichierController {

    private final FichierService fichierService;

    public FichierController(FichierService fichierService) {
        this.fichierService = fichierService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FichierDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categorie") FichierCategorie categorie,
            @RequestParam(value = "entiteType", required = false) EntiteType entiteType,
            @RequestParam(value = "entiteId", required = false) Long entiteId,
            @AuthenticationPrincipal SirhUserDetails principal) {
        Fichier fichier = fichierService.upload(file, categorie, entiteType, entiteId, principal.getUserId());
        return ResponseEntity.status(201).body(FichierDto.from(fichier));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Fichier fichier = fichierService.findById(id);
        Resource resource = fichierService.loadAsResource(fichier);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fichier.getNomOriginal())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @GetMapping("/entite/{type}/{id}")
    public ResponseEntity<List<FichierDto>> listByEntite(@PathVariable("type") EntiteType type,
                                                           @PathVariable("id") Long id) {
        List<FichierDto> dtos = fichierService.listByEntite(type, id).stream()
                .map(FichierDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fichierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
