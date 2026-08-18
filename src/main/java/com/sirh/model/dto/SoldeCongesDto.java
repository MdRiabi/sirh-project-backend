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
public class SoldeCongesDto {
  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("solde_conges_jours")
  private BigDecimal soldeCongesJours;
}
