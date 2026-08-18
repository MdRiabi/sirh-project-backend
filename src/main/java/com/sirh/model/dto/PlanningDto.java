package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.PlanningStatut;
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
public class PlanningDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("shift_id")
  private Long shiftId;

  @JsonProperty("shift_nom")
  private String shiftNom;

  @JsonProperty("date_debut")
  private LocalDateTime dateDebut;

  @JsonProperty("date_fin")
  private LocalDateTime dateFin;

  @JsonProperty("statut")
  private PlanningStatut statut;

  @JsonProperty("commentaire")
  private String commentaire;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
