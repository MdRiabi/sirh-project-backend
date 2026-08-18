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
public class DepartementDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("manager_id")
  private UUID managerId;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
