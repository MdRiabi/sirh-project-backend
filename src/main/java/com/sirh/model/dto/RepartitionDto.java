package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Répartition pour un graphique en anneau (donut) — libellé + effectif brut.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionDto {
  @JsonProperty("label")
  private String label;

  @JsonProperty("valeur")
  private long valeur;
}
