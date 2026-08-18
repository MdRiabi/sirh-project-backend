package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Vue consolidée du Dashboard (Module 10, Lot 11) : composé en lecture seule à
// partir des repositories de chaque module métier, aucune nouvelle FK.
// tauxAnomaliesJustifieesDernierMois est nullable (aucune clôture de paie
// encore réalisée en début de vie de l'application).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KpiDto {
  @JsonProperty("effectif_actif")
  private long effectifActif;

  @JsonProperty("effectif_total")
  private long effectifTotal;

  @JsonProperty("taux_turnover_mensuel")
  private BigDecimal tauxTurnoverMensuel;

  @JsonProperty("conges_en_cours")
  private long congesEnCours;

  @JsonProperty("alertes_pointage_non_justifiees")
  private long alertesPointageNonJustifiees;

  @JsonProperty("renouvellements_urgents")
  private long renouvellementsUrgents;

  @JsonProperty("evaluations_en_cours")
  private long evaluationsEnCours;

  @JsonProperty("evaluations_validees")
  private long evaluationsValidees;

  @JsonProperty("taux_anomalies_justifiees_dernier_mois")
  private BigDecimal tauxAnomaliesJustifieesDernierMois;
}
