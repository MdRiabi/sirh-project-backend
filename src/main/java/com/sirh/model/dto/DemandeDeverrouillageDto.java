package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// departementId = celui du demandeur (pas de département sur cloture_paie,
// qui reste globale par mois — cf. CloturePaieService), utilisé côté
// frontend pour filtrer la liste par le département affiché à l'écran.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DemandeDeverrouillageDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("mois")
  private LocalDate mois;

  @JsonProperty("motif")
  private String motif;

  @JsonProperty("demande_par_id")
  private UUID demandeParId;

  @JsonProperty("demande_par_nom")
  private String demandeParNom;

  @JsonProperty("demande_par_prenom")
  private String demandeParPrenom;

  @JsonProperty("departement_id")
  private Long departementId;

  @JsonProperty("statut")
  private String statut;

  @JsonProperty("traite_par_id")
  private UUID traiteParId;

  @JsonProperty("date_traitement")
  private LocalDateTime dateTraitement;
}
