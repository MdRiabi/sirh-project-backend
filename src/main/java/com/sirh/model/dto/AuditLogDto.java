package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// GET /audit-logs (SUPERUSER exclusivement) — utilisateurNom résolu par un
// lookup batché sur Utilisateur (AuditLogService), l'entité AuditLog elle-même
// ne porte que l'id brut (immuable, pas de relation JPA, cf. AuditLog.java).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("utilisateur_id")
  private Long utilisateurId;

  @JsonProperty("utilisateur_nom")
  private String utilisateurNom;

  @JsonProperty("action")
  private String action;

  @JsonProperty("table_cible")
  private String tableCible;

  @JsonProperty("anciennes_valeurs")
  private String anciennesValeurs;

  @JsonProperty("nouvelles_valeurs")
  private String nouvellesValeurs;

  @JsonProperty("adresse_ip")
  private String adresseIp;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
