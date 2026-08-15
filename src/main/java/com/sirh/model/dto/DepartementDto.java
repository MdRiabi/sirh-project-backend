package com.sirh.model.dto;

import com.sirh.model.entity.Departement;

import java.time.LocalDateTime;
import java.util.UUID;

public record DepartementDto(
        Long id,
        String nom,
        UUID managerId,
        LocalDateTime createdAt
) {
    public static DepartementDto from(Departement departement) {
        return new DepartementDto(
                departement.getId(),
                departement.getNom(),
                departement.getManagerId(),
                departement.getCreatedAt()
        );
    }
}
