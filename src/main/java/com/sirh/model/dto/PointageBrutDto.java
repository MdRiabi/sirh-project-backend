package com.sirh.model.dto;

import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;

import java.time.LocalDateTime;
import java.util.UUID;

public record PointageBrutDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        Long pointeuseId,
        String pointeuseNom,
        LocalDateTime timestampPointage,
        TypeEvenement typeEvenement,
        MethodeValidation methodeValidation,
        SourceImport sourceImport,
        PointageStatut statut,
        Long autorisationId,
        LocalDateTime createdAt
) {
}