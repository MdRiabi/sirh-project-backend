package com.sirh.demandes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record IndisponibiliteCreateRequest(
        @NotNull UUID employeId,
        @NotBlank String typeIndisponibilite,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        String commentaire,
        Long pieceJointeId,
        String avisManagerStatut,
        String avisManagerCommentaire
) {
}
