package com.sirh.controller;

import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.AsyncJobDto;
import com.sirh.model.dto.ReportGenerationResult;
import com.sirh.model.dto.ReportGenererRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

// Module 7 (CDF) : "Le RH sélectionne un type de rapport ... clique sur
// EXPORTER." — RH/SUPERUSER seulement (même scope que la génération de tout
// export sensible du projet, cf. import Excel pointages).
@Tag(
    name = "Rapports",
    description =
        "Génération et téléchargement de rapports (`TURNOVER_ANNUEL`, `ABSENTEISME_ANNUEL` —"
            + " formats PDF/EXCEL), adossée aux `async_jobs` (cahier-technique.md §4.8)")
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class ReportController {

  private final ReportGenerationService reportGenerationService;

  @Operation(
      summary = "Générer un rapport",
      description =
          "Catalogue limité à `TURNOVER_ANNUEL`, `ABSENTEISME_ANNUEL` (formats `PDF`/`EXCEL`,"
              + " `departementId` optionnel). Si le résultat n'est pas prêt sous 5s, la requête"
              + " bascule en 202 avec suivi via `async_jobs` au lieu de bloquer.")
  @PostMapping("/generer")
  public ResponseEntity<?> generer(
      @Valid @RequestBody ReportGenererRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    ReportGenerationResult resultat =
        reportGenerationService.genererRapport(request, principal.getUserId());
    return switch (resultat) {
      case ReportGenerationResult.Pret pret ->
          ResponseEntity.ok()
              .contentType(pret.rapport().getContentType())
              .header(
                  HttpHeaders.CONTENT_DISPOSITION,
                  ContentDisposition.attachment()
                      .filename(pret.rapport().getNomFichier())
                      .build()
                      .toString())
              .body(pret.rapport().getContenu());
      case ReportGenerationResult.Lance lance ->
          ResponseEntity.status(HttpStatus.ACCEPTED).body(SingleObjectDto.of(lance.job()));
    };
  }

  @Operation(summary = "Consulter l'état d'un job de génération de rapport")
  @GetMapping("/tasks/{reference}")
  public ResponseEntity<SingleObjectDto<AsyncJobDto>> tache(@PathVariable UUID reference) {
    return ResponseEntity.ok(SingleObjectDto.of(reportGenerationService.obtenirTache(reference)));
  }

  // 🆕 non listé au cahier technique (seuls POST /reports/generer et
  // GET /reports/tasks/{reference} y figurent) — nécessaire pour récupérer le
  // fichier une fois le job async_jobs passé à SUCCESS, même principe que
  // GET /fichiers/{reference}/download.
  @Operation(
      summary = "Télécharger le fichier d'un rapport généré",
      description = "409 si le job n'est pas encore au statut `SUCCESS`.")
  @GetMapping("/tasks/{reference}/download")
  public ResponseEntity<Resource> telecharger(@PathVariable UUID reference) {
    ReportGenerationService.FichierTelecharge fichier =
        reportGenerationService.chargerFichierResultat(reference);
    return ResponseEntity.ok()
        .contentType(fichier.contentType())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fichier.nomFichier()).build().toString())
        .body(fichier.resource());
  }
}
