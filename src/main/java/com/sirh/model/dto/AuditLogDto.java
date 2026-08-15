package com.sirh.model.dto;

import java.time.LocalDateTime;

// GET /audit-logs (SUPERUSER exclusivement) — utilisateurNom résolu par un
// lookup batché sur Utilisateur (AuditLogService), l'entité AuditLog elle-même
// ne porte que l'id brut (immuable, pas de relation JPA, cf. AuditLog.java).
public record AuditLogDto(
        Long id,
        Long utilisateurId,
        String utilisateurNom,
        String action,
        String tableCible,
        String anciennesValeurs,
        String nouvellesValeurs,
        String adresseIp,
        LocalDateTime createdAt
) {
}
