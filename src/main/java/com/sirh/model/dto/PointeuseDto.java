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
public class PointeuseDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("adresse_ip")
  private String adresseIp;

  @JsonProperty("port")
  private Integer port;

  @JsonProperty("batiment")
  private String batiment;

  @JsonProperty("statut_en_ligne")
  private Boolean statutEnLigne;

  @JsonProperty("derniere_synchro")
  private LocalDateTime derniereSynchro;
}
