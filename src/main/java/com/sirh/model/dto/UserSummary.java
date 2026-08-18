package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
  @JsonProperty("user_id")
  private Long userId;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("username")
  private String username;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("prenom")
  private String prenom;

  @JsonProperty("role")
  private String role;

  @JsonProperty("departement_id")
  private Long departementId;
}
