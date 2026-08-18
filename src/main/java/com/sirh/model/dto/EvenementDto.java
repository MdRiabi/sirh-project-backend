package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.enums.StatutBadge;
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
public class EvenementDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("titre")
  private String titre;

  @JsonProperty("description")
  private String description;

  @JsonProperty("categorie")
  private EvenementCategorie categorie;

  @JsonProperty("statut_badge")
  private StatutBadge statutBadge;

  @JsonProperty("saisi_par_id")
  private Long saisiParId;

  @JsonProperty("date_evenement")
  private LocalDateTime dateEvenement;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
