package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("type_rotation")
  private String typeRotation;

  @JsonProperty("heure_debut")
  private LocalTime heureDebut;

  @JsonProperty("heure_fin")
  private LocalTime heureFin;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
