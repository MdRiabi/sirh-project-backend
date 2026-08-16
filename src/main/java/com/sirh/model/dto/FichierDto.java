package com.sirh.model.dto;

import com.sirh.model.enums.EntiteType;
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
}
