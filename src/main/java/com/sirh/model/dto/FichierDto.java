package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.model.enums.FichierStatut;
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
public class FichierDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("nom_original")
  private String nomOriginal;

  @JsonProperty("type_mime")
  private String typeMime;

  @JsonProperty("taille_octets")
  private Long tailleOctets;

  @JsonProperty("categorie")
  private FichierCategorie categorie;

  @JsonProperty("statut")
  private FichierStatut statut;

  @JsonProperty("entite_type")
  private EntiteType entiteType;

  @JsonProperty("entite_id")
  private Long entiteId;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
