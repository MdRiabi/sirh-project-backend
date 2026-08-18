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
public class ParametreGlobalDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("cle")
  private String cle;

  @JsonProperty("valeur")
  private String valeur;

  @JsonProperty("type")
  private String type;

  @JsonProperty("description")
  private String description;

  @JsonProperty("updated_at")
  private LocalDateTime updatedAt;
}
