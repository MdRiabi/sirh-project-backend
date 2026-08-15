package com.sirh.model.dto;

import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.entity.PointageBrut;
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
    public static PointageBrutDto from(PointageBrut p) {
        return from(p, null, null, null);
    }

    public static PointageBrutDto from(PointageBrut p, String employeNom, String employePrenom, String pointeuseNom) {
        return new PointageBrutDto(
                p.getReference(),
                p.getEmployeId(),
                employeNom,
                employePrenom,
                p.getPointeuseId(),
                pointeuseNom,
                p.getTimestampPointage(),
                p.getTypeEvenement(),
                p.getMethodeValidation(),
                p.getSourceImport(),
                p.getStatut(),
                p.getAutorisationId(),
                p.getCreatedAt()
        );
    }
}