package com.sirh.controller;

import com.sirh.model.dto.AsyncJobDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.ReportGenererRequest;
import com.sirh.model.dto.ReportGenerationResult;
import com.sirh.service.ReportGenerationService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Module 7 (CDF) : "Le RH sélectionne un type de rapport ... clique sur
// EXPORTER." — RH/SUPERUSER seulement (même scope que la génération de tout
// export sensible du projet, cf. import Excel pointages).
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    public ReportController(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    @PostMapping("/generer")
    public ResponseEntity<?> generer(@Valid @RequestBody ReportGenererRequest request,
                                      @AuthenticationPrincipal SirhUserDetails principal) {
        ReportGenerationResult resultat = reportGenerationService.genererRapport(request, principal.getUserId());
        return switch (resultat) {
            case ReportGenerationResult.Pret pret -> ResponseEntity.ok()
                    .contentType(pret.rapport().contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(pret.rapport().nomFichier()).build().toString())
                    .body(pret.rapport().contenu());
            case ReportGenerationResult.Lance lance -> ResponseEntity.status(HttpStatus.ACCEPTED).body(lance.job());
        };
    }

    @GetMapping("/tasks/{reference}")
    public ResponseEntity<AsyncJobDto> tache(@PathVariable UUID reference) {
        return ResponseEntity.ok(reportGenerationService.obtenirTache(reference));
    }

    // 🆕 non listé au cahier technique (seuls POST /reports/generer et
    // GET /reports/tasks/{reference} y figurent) — nécessaire pour récupérer le
    // fichier une fois le job async_jobs passé à SUCCESS, même principe que
    // GET /fichiers/{reference}/download.
    @GetMapping("/tasks/{reference}/download")
    public ResponseEntity<Resource> telecharger(@PathVariable UUID reference) {
        ReportGenerationService.FichierTelecharge fichier = reportGenerationService.chargerFichierResultat(reference);
        return ResponseEntity.ok()
                .contentType(fichier.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier.nomFichier()).build().toString())
                .body(fichier.resource());
    }
}
