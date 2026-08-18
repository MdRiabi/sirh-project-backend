package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.AsyncJobStatut;
import com.sirh.model.enums.AsyncJobType;
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
public class AsyncJobDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("type")
  private AsyncJobType type;

  @JsonProperty("nom_fichier")
  private String nomFichier;

  @JsonProperty("statut")
  private AsyncJobStatut statut;

  @JsonProperty("progression")
  private Integer progression;

  @JsonProperty("resultat_url")
  private String resultatUrl;

  @JsonProperty("nombre_lignes_traitees")
  private Integer nombreLignesTraitees;

  @JsonProperty("ligne_erreur")
  private Integer ligneErreur;

  @JsonProperty("message_erreur")
  private String messageErreur;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  @JsonProperty("completed_at")
  private LocalDateTime completedAt;
}
