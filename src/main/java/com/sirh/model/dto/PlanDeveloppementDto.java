package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PlanDeveloppementDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("competence")
  private String competence;

  @JsonProperty("actions")
  private String actions;

  @JsonProperty("date_creation")
  private LocalDateTime dateCreation;

  @JsonProperty("statut")
  private String statut;
}
