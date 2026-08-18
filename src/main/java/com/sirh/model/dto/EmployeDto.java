package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class EmployeDto {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("matricule_interne")
  private String matriculeInterne;

  @JsonProperty("matricule_cnss")
  private String matriculeCnss;

  @JsonProperty("matricule_paie")
  private String matriculePaie;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("prenom")
  private String prenom;

  @JsonProperty("email")
  private String email;

  @JsonProperty("departement_id")
  private Long departementId;

  @JsonProperty("departement_nom")
  private String departementNom;

  @JsonProperty("type_contrat")
  private String typeContrat;

  @JsonProperty("date_embauche")
  private LocalDate dateEmbauche;

  @JsonProperty("date_fin_contrat")
  private LocalDate dateFinContrat;

  @JsonProperty("solde_conges_jours")
  private BigDecimal soldeCongesJours;

  @JsonProperty("actif")
  private Boolean actif;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
