package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;
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
public class PointageBrutDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("pointeuse_id")
  private Long pointeuseId;

  @JsonProperty("pointeuse_nom")
  private String pointeuseNom;

  @JsonProperty("timestamp_pointage")
  private LocalDateTime timestampPointage;

  @JsonProperty("type_evenement")
  private TypeEvenement typeEvenement;

  @JsonProperty("methode_validation")
  private MethodeValidation methodeValidation;

  @JsonProperty("source_import")
  private SourceImport sourceImport;

  @JsonProperty("statut")
  private PointageStatut statut;

  @JsonProperty("autorisation_id")
  private Long autorisationId;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
