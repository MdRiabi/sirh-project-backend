package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Variables de paie du mois pour un employé (Module 3, "traduire les
// présences en variables de paie") — pas un montant : aucun champ salaire
// n'existe sur employes, le calcul est destiné à l'export vers un logiciel de
// paie externe (cf. matricule_paie, ARCHITECTURE.md §1). heuresNormales +
// heuresMajorees = total travaillé ; heuresNuit est une étiquette
// indépendante (peut chevaucher l'une ou l'autre) utilisée pour une
// majoration de nuit calculée en aval.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalculMensuelEmployeDto {
  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("nom")
  private String nom;

  @JsonProperty("prenom")
  private String prenom;

  @JsonProperty("heures_normales")
  private BigDecimal heuresNormales;

  @JsonProperty("heures_nuit")
  private BigDecimal heuresNuit;

  @JsonProperty("heures_majorees")
  private BigDecimal heuresMajorees;

  @JsonProperty("taux_presence")
  private BigDecimal tauxPresence;

  @JsonProperty("taux_prime")
  private BigDecimal tauxPrime;

  @JsonProperty("incidents")
  private int incidents;

  @JsonProperty("penalite_heures")
  private BigDecimal penaliteHeures;
}
