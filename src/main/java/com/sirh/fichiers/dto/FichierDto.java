package com.sirh.fichiers.dto;

import com.sirh.fichiers.entity.EntiteType;
import com.sirh.fichiers.entity.Fichier;
import com.sirh.fichiers.entity.FichierCategorie;

import java.time.LocalDateTime;

public record FichierDto(
        Long id,
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
                fichier.getId(),
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
