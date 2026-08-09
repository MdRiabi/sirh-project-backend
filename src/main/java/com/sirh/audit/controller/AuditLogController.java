package com.sirh.audit.controller;

import com.sirh.audit.dto.AuditLogDto;
import com.sirh.audit.service.AuditLogService;
import org.springframework.data.domain.Page;
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

import java.time.LocalDateTime;

// cahier-technique.md §2.2/§4.8 : SUPERUSER exclusivement, RH retiré (seule
// exception à la règle "RH ⊂ SUPERUSER" du projet, constat 2 de
// docs/specs/audit-rapport.md). Premier contrôleur du projet restreint à un
// seul rôle (aucun hasAnyAuthority).
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")
    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> rechercher(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tableCible,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.rechercher(action, tableCible, utilisateurId, dateDebut, dateFin, pageable));
    }
}
