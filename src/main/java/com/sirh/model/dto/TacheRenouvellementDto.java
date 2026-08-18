package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.TacheRenouvellementStatut;
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
public class TacheRenouvellementDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("contrat_id")
  private Long contratId;

  @JsonProperty("type_echeance")
  private String typeEcheance;

  @JsonProperty("date_echeance")
  private LocalDate dateEcheance;

  @JsonProperty("statut")
  private TacheRenouvellementStatut statut;

  @JsonProperty("date_traitement")
  private LocalDateTime dateTraitement;

  @JsonProperty("traite_par_id")
  private Long traiteParId;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
