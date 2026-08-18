package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.model.dto.AuditLogDto;
import com.sirh.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// cahier-technique.md §2.2/§4.8 : SUPERUSER exclusivement, RH retiré (seule
// exception à la règle "RH ⊂ SUPERUSER" du projet, constat 2 de
// docs/specs/audit-rapport.md). Premier contrôleur du projet restreint à un
// seul rôle (aucun hasAnyAuthority).
@Tag(
    name = "Audit",
    description =
        "Logs d'audit immuables — accès exclusif SUPERUSER, le RH n'y a jamais accès"
            + " (cahier-technique.md §4.8)")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditLogService auditLogService;

  @Operation(
      summary = "Rechercher dans les logs d'audit",
      description =
          "Réservé exclusivement à SUPERUSER — le RH n'a jamais accès aux logs d'audit, même en"
              + " lecture seule (constat #2, audit-rapport.md).")
  @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")
  @GetMapping
  public ResponseEntity<ListingPageDto<AuditLogDto>> rechercher(
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String tableCible,
      @RequestParam(required = false) Long utilisateurId,
      @Parameter(description = "Format ISO 8601 (`yyyy-MM-ddTHH:mm:ss`), borne inférieure incluse")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateDebut,
      @Parameter(description = "Format ISO 8601 (`yyyy-MM-ddTHH:mm:ss`), borne supérieure incluse")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateFin,
      @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            auditLogService.rechercher(
                action, tableCible, utilisateurId, dateDebut, dateFin, pageable)));
  }
}
