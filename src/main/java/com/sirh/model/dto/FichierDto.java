package com.sirh.model.dto;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.FichierCategorie;

import java.time.LocalDateTime;
import java.util.UUID;

public record FichierDto(
        UUID reference,
        String nomOriginal,
        String typeMime,
        Long tailleOctets,
        FichierCategorie categorie,
        EntiteType entiteType,
        Long entiteId,
        LocalDateTime createdAt
) {
    public static FichierDto from(Fichier fichier) {
        return new FichierDto(
                fichier.getReference(),
                fichier.getNomOriginal(),
                fichier.getTypeMime(),
                fichier.getTailleOctets(),
                fichier.getCategorie(),
                fichier.getEntiteType(),
                fichier.getEntiteId(),
                fichier.getCreatedAt()
        );
    }
}
