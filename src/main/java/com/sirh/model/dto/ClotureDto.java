package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClotureDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("mois")
  private LocalDate mois;

  @JsonProperty("statut")
  private String statut;

  @JsonProperty("date_cloture")
  private LocalDateTime dateCloture;

  @JsonProperty("cloture_par_id")
  private UUID clotureParId;

  @JsonProperty("taux_anomalies_justifiees")
  private BigDecimal tauxAnomaliesJustifiees;
}
