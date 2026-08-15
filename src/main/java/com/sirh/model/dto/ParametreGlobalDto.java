package com.sirh.model.dto;

import com.sirh.model.entity.ParametreGlobal;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParametreGlobalDto(
        UUID reference,
        String cle,
        String valeur,
        String type,
        String description,
        LocalDateTime updatedAt
) {
    public static ParametreGlobalDto from(ParametreGlobal p) {
        return new ParametreGlobalDto(p.getReference(), p.getCle(), p.getValeur(), p.getType(), p.getDescription(), p.getUpdatedAt());
    }
}
