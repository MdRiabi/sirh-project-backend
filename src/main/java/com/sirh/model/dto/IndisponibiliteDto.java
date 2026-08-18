package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class IndisponibiliteDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("employe_id")
  private UUID employeId;

  @JsonProperty("employe_nom")
  private String employeNom;

  @JsonProperty("employe_prenom")
  private String employePrenom;

  @JsonProperty("type_indisponibilite")
  private String typeIndisponibilite;

  @JsonProperty("date_debut")
  private LocalDate dateDebut;

  @JsonProperty("date_fin")
  private LocalDate dateFin;

  @JsonProperty("statut")
  private String statut;

  @JsonProperty("commentaire")
  private String commentaire;

  @JsonProperty("piece_jointe_id")
  private Long pieceJointeId;

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

  @JsonProperty("jours_ouvres_decomptes")
  private Integer joursOuvresDecomptes;
}
