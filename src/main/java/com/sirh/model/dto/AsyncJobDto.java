package com.sirh.model.dto;

import com.sirh.model.enums.AsyncJobStatut;
import com.sirh.model.enums.AsyncJobType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsyncJobDto(
        UUID reference,
        AsyncJobType type,
        String nomFichier,
        AsyncJobStatut statut,
        Integer progression,
        String resultatUrl,
        Integer nombreLignesTraitees,
        Integer ligneErreur,
        String messageErreur,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}