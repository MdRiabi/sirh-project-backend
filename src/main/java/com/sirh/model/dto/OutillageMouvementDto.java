package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.EtatMouvement;
import com.sirh.model.enums.TypeMouvement;
import java.math.BigDecimal;
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
public class OutillageMouvementDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("outillage_id")
  private Long outillageId;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("type_mouvement")
  private TypeMouvement typeMouvement;

  @JsonProperty("etat")
  private EtatMouvement etat;

  @JsonProperty("montant_imputation")
  private BigDecimal montantImputation;

  @JsonProperty("commentaire")
  private String commentaire;

  @JsonProperty("effectue_par_id")
  private Long effectueParId;

  @JsonProperty("date_mouvement")
  private LocalDateTime dateMouvement;
}
