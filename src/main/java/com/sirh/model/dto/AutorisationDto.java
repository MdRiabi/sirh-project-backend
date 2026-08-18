package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
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
public class AutorisationDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("type")
  private String type;

  @JsonProperty("date_autorisation")
  private LocalDate dateAutorisation;

  @JsonProperty("heure_debut")
  private LocalTime heureDebut;

  @JsonProperty("heure_fin")
  private LocalTime heureFin;

  @JsonProperty("duree_minutes")
  private Integer dureeMinutes;

  @JsonProperty("motif")
  private String motif;

  @JsonProperty("statut")
  private String statut;

  @JsonProperty("cree_par_id")
  private UUID creeParId;

  @JsonProperty("cree_par_role")
  private String creeParRole;

  @JsonProperty("avis_manager_statut")
  private String avisManagerStatut;

  @JsonProperty("avis_manager_commentaire")
  private String avisManagerCommentaire;

  @JsonProperty("valide_par_rh_id")
  private UUID valideParRhId;

  @JsonProperty("quota_depasse_minutes")
  private Integer quotaDepasseMinutes;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
