package com.sirh.model.dto;

import com.sirh.model.entity.Departement;

import java.time.LocalDateTime;
import java.util.UUID;

public record DepartementDto(
        UUID reference,
        String nom,
        UUID managerId,
        LocalDateTime createdAt
) {
    public static DepartementDto from(Departement departement) {
        return new DepartementDto(
                departement.getReference(),
                departement.getNom(),
                departement.getManagerId(),
                departement.getCreatedAt()
        );
    }
}
