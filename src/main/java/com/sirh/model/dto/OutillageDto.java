package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.OutillageEtat;
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
public class OutillageDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("code_barres")
  private String codeBarres;

  @JsonProperty("nom_outil")
  private String nomOutil;

  @JsonProperty("etat")
  private OutillageEtat etat;

  @JsonProperty("attribue_a_employe_id")
  private UUID attribueAEmployeId;

  @JsonProperty("attribue_a_employe_nom")
  private String attribueAEmployeNom;

  @JsonProperty("attribue_a_employe_prenom")
  private String attribueAEmployePrenom;

  @JsonProperty("date_attribution")
  private LocalDateTime dateAttribution;

  @JsonProperty("departement_id")
  private Long departementId;

  @JsonProperty("valeur_achat")
  private BigDecimal valeurAchat;

  @JsonProperty("date_achat")
  private LocalDate dateAchat;
}
