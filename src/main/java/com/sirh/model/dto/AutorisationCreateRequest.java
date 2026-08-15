package com.sirh.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// avisManagerStatut/avisManagerCommentaire : obligatoire si le créateur est
// MANAGER, ignoré si RH (workflow d'urgence) — validé en service, pas en
// contrainte Bean Validation statique (dépend du rôle de l'appelant, connu
// seulement au moment de l'appel).
public record AutorisationCreateRequest(
        @NotNull UUID employeId,
        @NotBlank String type,
        @NotNull LocalDate dateAutorisation,
        @NotNull LocalTime heureDebut,
        LocalTime heureFin,
        @NotNull @Positive Integer dureeMinutes,
        String motif,
        String avisManagerStatut,
        String avisManagerCommentaire
) {
}
