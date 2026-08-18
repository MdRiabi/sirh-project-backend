package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.CorrectionPointageRequest;
import com.sirh.model.dto.PointageBrutDto;
import com.sirh.model.enums.PointageStatut;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.ImportPointagesService;
import com.sirh.service.PointageBrutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(
    name = "Pointages",
    description =
        "Consultation, correction et import Excel des pointages bruts (cahier-technique.md §4.5)."
            + " Doublon détecté sur une fenêtre de 5 minutes, même employé + même pointeuse — seul"
            + " le 1er pointage compte pour les heures travaillées.")
@RestController
@RequestMapping("/pointages")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class PointageController {

  private final PointageBrutService pointageBrutService;
  private final ImportPointagesService importPointagesService;

  @Operation(summary = "Lister les pointages bruts")
  @GetMapping
  public ResponseEntity<ListingPageDto<PointageBrutDto>> list(
      @RequestParam(required = false) UUID employeId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateDebut,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateFin,
      @RequestParam(required = false) PointageStatut statut) {
    return ResponseEntity.ok(
        ListingPageDto.of(pointageBrutService.search(employeId, dateDebut, dateFin, statut)));
  }

  @Operation(
      summary = "Importer des pointages depuis un fichier Excel",
      description =
          "Traitement asynchrone (job suivi dans `async_jobs`, cf. `GET"
              + " /async-jobs?type=IMPORT_POINTAGES`). Rollback total au premier échec — jamais"
              + " d'insertion partielle.")
  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<SingleObjectDto<Map<String, UUID>>> importer(
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var job = importPointagesService.creerJob(file, principal.getUserId());
    return ResponseEntity.accepted().body(SingleObjectDto.of(Map.of("jobId", job.getReference())));
  }

  @Operation(summary = "Corriger un pointage")
  @PutMapping("/{reference}/corriger")
  public ResponseEntity<SingleObjectDto<PointageBrutDto>> corrigerPointageByReference(
      @PathVariable UUID reference,
      @Valid @RequestBody CorrectionPointageRequest request,
      @AuthenticationPrincipal SirhUserDetails principal,
      HttpServletRequest httpRequest) {
    var pointage =
        pointageBrutService.corrigerPointageByReference(
            reference, request, principal.getUserId(), httpRequest.getRemoteAddr());
    return ResponseEntity.ok(SingleObjectDto.of(pointage));
  }
}
