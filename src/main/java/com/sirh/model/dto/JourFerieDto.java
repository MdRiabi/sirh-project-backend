package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.TypeJourFerie;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JourFerieDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("date")
  private LocalDate date;

  @JsonProperty("libelle")
  private String libelle;

  @JsonProperty("type")
  private TypeJourFerie type;

  @JsonProperty("national")
  private Boolean national;

  @JsonProperty("region")
  private String region;
}
