package com.sirh.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PointeuseDto(
        UUID reference,
        String nom,
        String adresseIp,
        Integer port,
        String batiment,
        Boolean statutEnLigne,
        LocalDateTime derniereSynchro
) {
}