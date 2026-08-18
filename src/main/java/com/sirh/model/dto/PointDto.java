package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Point générique d'une série temporelle (courbe/histogramme) — mois (YYYY-MM)
// ou semaine (YYYY-MM-DD du lundi) selon le graphique, cf. ChartsDto.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointDto {
  @JsonProperty("label")
  private String label;

  @JsonProperty("valeur")
  private BigDecimal valeur;
}
