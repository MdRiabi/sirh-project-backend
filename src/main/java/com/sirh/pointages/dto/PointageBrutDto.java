package com.sirh.pointages.dto;

import com.sirh.pointages.entity.MethodeValidation;
import com.sirh.pointages.entity.PointageBrut;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.entity.SourceImport;
import com.sirh.pointages.entity.TypeEvenement;

import java.time.LocalDateTime;
import java.util.UUID;

public record PointageBrutDto(
        Long id,
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
                p.getId(),
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