package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCompetenceDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("competence")
  private String competence;

  @JsonProperty("note")
  private BigDecimal note;

  @JsonProperty("seuil_exigence")
  private BigDecimal seuilExigence;

  @JsonProperty("commentaire")
  private String commentaire;
}
