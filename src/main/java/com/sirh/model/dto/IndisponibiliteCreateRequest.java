package com.sirh.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// forcer : ignoré sauf pour une saisie RH directe (workflow d'urgence, statut
// VALIDE dès la création) — déduit le solde même s'il devient négatif. Même
// rôle que IndisponibiliteValiderRequest.forcer pour le workflow normal en
// deux temps.
public record IndisponibiliteCreateRequest(
        @NotNull UUID employeId,
        @NotBlank String typeIndisponibilite,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        String commentaire,
        Long pieceJointeId,
        String avisManagerStatut,
        String avisManagerCommentaire,
        Boolean forcer
) {
}
