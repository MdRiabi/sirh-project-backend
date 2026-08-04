package com.sirh.parametres.dto;

import com.sirh.parametres.entity.ParametreGlobal;

import java.time.LocalDateTime;

public record ParametreGlobalDto(
        Long id,
        String cle,
        String valeur,
        String type,
        String description,
        LocalDateTime updatedAt
) {
    public static ParametreGlobalDto from(ParametreGlobal p) {
        return new ParametreGlobalDto(p.getId(), p.getCle(), p.getValeur(), p.getType(), p.getDescription(), p.getUpdatedAt());
    }
}
