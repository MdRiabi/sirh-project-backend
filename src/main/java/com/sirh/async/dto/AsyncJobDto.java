package com.sirh.async.dto;

import com.sirh.async.entity.AsyncJob;
import com.sirh.async.entity.AsyncJobStatut;
import com.sirh.async.entity.AsyncJobType;

import java.time.LocalDateTime;

public record AsyncJobDto(
        Long id,
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
    public static AsyncJobDto from(AsyncJob job) {
        return new AsyncJobDto(
                job.getId(),
                job.getType(),
                job.getNomFichier(),
                job.getStatut(),
                job.getProgression(),
                job.getResultatUrl(),
                job.getNombreLignesTraitees(),
                job.getLigneErreur(),
                job.getMessageErreur(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}