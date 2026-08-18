package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.CorrectionPointageRequest;
import com.sirh.model.dto.PointageBrutDto;
import com.sirh.model.enums.PointageStatut;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.ImportPointagesService;
import com.sirh.service.PointageBrutService;
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

@RestController
@RequestMapping("/pointages")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class PointageController {

  private final PointageBrutService pointageBrutService;
  private final ImportPointagesService importPointagesService;

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

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<SingleObjectDto<Map<String, UUID>>> importer(
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var job = importPointagesService.creerJob(file, principal.getUserId());
    return ResponseEntity.accepted().body(SingleObjectDto.of(Map.of("jobId", job.getReference())));
  }

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
