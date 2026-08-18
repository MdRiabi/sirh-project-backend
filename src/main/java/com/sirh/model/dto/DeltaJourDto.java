package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Rapport croisé planning théorique / pointage réel (cahier-fonctionnel.md
// Module 2 — "Génération d'un rapport croisé (Delta)").
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeltaJourDto {
  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("jour")
  private LocalDate jour;

  @JsonProperty("heures_theoriques")
  private BigDecimal heuresTheoriques;

  @JsonProperty("heures_reelles")
  private BigDecimal heuresReelles;

  @JsonProperty("delta")
  private BigDecimal delta;
}
