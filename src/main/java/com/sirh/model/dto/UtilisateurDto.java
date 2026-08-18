package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.Role;
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
public class UtilisateurDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe")
  private EmployeDto employe;

  @JsonProperty("username")
  private String username;

  @JsonProperty("role")
  private Role role;

  @JsonProperty("compte_actif")
  private Boolean compteActif;

  @JsonProperty("dernier_login")
  private LocalDateTime dernierLogin;

  @JsonProperty("verrouille_jusqu_a")
  private LocalDateTime verrouilleJusquA;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
