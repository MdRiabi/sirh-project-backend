package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.CalculMensuelEmployeDto;
import com.sirh.model.dto.ClotureDto;
import com.sirh.model.dto.ClotureRequest;
import com.sirh.model.dto.DemandeDeverrouillageCreateRequest;
import com.sirh.model.dto.DemandeDeverrouillageDto;
import com.sirh.model.dto.DemandeDeverrouillageTraiterRequest;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.AssiduiteCalculService;
import com.sirh.service.CloturePaieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Assiduité & Clôture paie",
    description =
        "Calcul mensuel d'assiduité, clôture de paie irréversible et demandes de déverrouillage"
            + " exceptionnel (cahier-technique.md §4.7, §3.25, §3.26)")
@RestController
@RequestMapping("/assiduite")
@RequiredArgsConstructor
public class AssiduiteController {

  private final AssiduiteCalculService assiduiteCalculService;
  private final CloturePaieService cloturePaieService;

  // MANAGER restreint à son propre département, filtres client ignorés — même
  // convention que GET /autorisations, GET /indisponibilites (Lot 5).
  @Operation(
      summary = "Calculer l'assiduité mensuelle par employé",
      description =
          "Un `CalculMensuelEmployeDto` par employé (heures normales/nuit/majorées, taux de"
              + " présence, taux de prime, incidents, pénalité) — aucun montant monétaire, ces"
              + " données alimentent la paie externe via `matricule_paie`. MANAGER restreint à"
              + " son propre département (`departementId` client ignoré).")
  @GetMapping("/calcul-mensuel")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<ListingPageDto<CalculMensuelEmployeDto>> calculMensuel(
      @Parameter(description = "Mois au format `YYYY-MM`") @RequestParam String mois,
      @Parameter(description = "Ignoré pour MANAGER (scope forcé à son propre département)")
          @RequestParam(required = false)
          Long departementId,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(
            assiduiteCalculService.calculerMoisAvecScope(
                YearMonth.parse(mois),
                principal.getRole(),
                principal.getDepartementId(),
                departementId)));
  }

  // Lecture seule du statut de clôture, sans forcer la clôture ni exiger
  // qu'une ligne existe déjà (mois jamais clôturé -> OUVERT) — mêmes rôles
  // que calcul-mensuel, pas de scope département (la clôture est globale).
  @Operation(
      summary = "Consulter le statut de clôture paie d'un mois",
      description =
          "Renvoie le `ClotureDto` existant, ou un statut par défaut `OUVERT`"
              + " (`id`/`dateCloture`/`clotureParId`/`tauxAnomaliesJustifiees` à `null`) si aucune"
              + " clôture n'existe encore pour ce mois — jamais de 404.")
  @GetMapping("/cloture-paie")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
  public ResponseEntity<SingleObjectDto<ClotureDto>> statutCloture(
      @Parameter(description = "Mois au format `YYYY-MM`") @RequestParam String mois) {
    return ResponseEntity.ok(
        SingleObjectDto.of(cloturePaieService.trouverOuDefaut(YearMonth.parse(mois))));
  }

  @Operation(
      summary = "Clôturer la paie d'un mois",
      description =
          "Action irréversible. 409 si le taux d'anomalies justifiées est inférieur à"
              + " `SEUIL_ANOMALIES_CLOTURE`, si une autorisation ou indisponibilité du mois est"
              + " encore `EN_ATTENTE_RH`, ou si le mois est déjà `CLOTURE`.")
  @PostMapping("/cloturer-paie")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<ClotureDto>> cloturerPaie(
      @Valid @RequestBody ClotureRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            cloturePaieService.cloturer(
                YearMonth.parse(request.mois()), principal.getEmployeId())));
  }

  @Operation(
      summary = "Demander un déverrouillage exceptionnel d'un mois clôturé",
      description = "400 si le mois n'est pas au statut `CLOTURE`.")
  @PostMapping("/demandes-deverrouillage")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<DemandeDeverrouillageDto>> demanderDeverrouillage(
      @Valid @RequestBody DemandeDeverrouillageCreateRequest request,
      @AuthenticationPrincipal SirhUserDetails principal) {
    var demande =
        cloturePaieService.demanderDeverrouillage(
            YearMonth.parse(request.mois()), request.motif(), principal.getEmployeId());
    return ResponseEntity.status(201).body(SingleObjectDto.of(demande));
  }

  // Ouvert à MANAGER (scope forcé à son propre département, même convention
  // que GET /autorisations) : nécessaire pour qu'il puisse savoir si une
  // demande EN_ATTENTE existe déjà pour le mois/département affiché (évite
  // les doublons de soumission côté frontend).
  @Operation(
      summary = "Lister les demandes de déverrouillage",
      description =
          "Filtres optionnels combinables. MANAGER restreint à son propre département (résolu via"
              + " son `departementId`, filtre client ignoré).")
  @GetMapping("/demandes-deverrouillage")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<ListingPageDto<DemandeDeverrouillageDto>> listerDemandesDeverrouillage(
      @Parameter(description = "Mois au format `YYYY-MM`") @RequestParam(required = false)
          String mois,
      @Parameter(description = "Ignoré pour MANAGER (scope forcé à son propre département)")
          @RequestParam(required = false)
          Long departementId,
      @Parameter(description = "Filtre optionnel sur le statut de la demande")
          @RequestParam(required = false)
          String statut,
      @AuthenticationPrincipal SirhUserDetails principal) {
    YearMonth moisParse = mois != null ? YearMonth.parse(mois) : null;
    return ResponseEntity.ok(
        ListingPageDto.of(
            cloturePaieService.listerDemandesAvecScope(
                principal.getRole(),
                principal.getDepartementId(),
                moisParse,
                departementId,
                statut)));
  }

  @Operation(
      summary = "Approuver ou refuser une demande de déverrouillage",
      description =
          "409 si la demande n'est pas au statut `EN_ATTENTE`. Une approbation (`APPROUVEE`)"
              + " rouvre le mois (`cloture_paie.statut = OUVERT`).")
  @PutMapping("/demandes-deverrouillage/{reference}")
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<DemandeDeverrouillageDto>>
      traiterDemandeDeverrouillageByReference(
          @PathVariable UUID reference,
          @Valid @RequestBody DemandeDeverrouillageTraiterRequest request,
          @AuthenticationPrincipal SirhUserDetails principal) {
    var demande =
        cloturePaieService.traiterDemandeByReference(
            reference, request.statut(), principal.getEmployeId());
    return ResponseEntity.ok(SingleObjectDto.of(demande));
  }
}
