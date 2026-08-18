package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.EvaluationStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("manager_id")
  private UUID managerId;

  @JsonProperty("annee")
  private Integer annee;

  @JsonProperty("statut")
  private EvaluationStatut statut;

  @JsonProperty("date_entretien")
  private LocalDate dateEntretien;

  @JsonProperty("date_validation_rh")
  private LocalDateTime dateValidationRh;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  @JsonProperty("competences")
  private List<EvaluationCompetenceDto> competences;

  @JsonProperty("competences_manquantes")
  private List<String> competencesManquantes;

  @JsonProperty("plans_developpement")
  private List<PlanDeveloppementDto> plansDeveloppement;
}
