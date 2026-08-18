package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// GET /dashboard/charts (Lot 11) — 🆕 non listé au §4.8 de cahier-technique.md
// (seul /dashboard/kpi y figure), ajouté pour les visualisations du Tableau de
// Bord (CDF Module 10) et documenté a posteriori, même principe que les gaps
// comblés lot par lot (ex. GET /plannings/delta au Lot 3).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChartsDto {
  @JsonProperty("evolution_effectifs")
  private List<PointDto> evolutionEffectifs;

  @JsonProperty("turnover_mensuel")
  private List<PointDto> turnoverMensuel;

  @JsonProperty("conges_en_cours")
  private List<RepartitionDto> congesEnCours;

  @JsonProperty("anomalies_pointage_hebdo")
  private List<PointDto> anomaliesPointageHebdo;
}
