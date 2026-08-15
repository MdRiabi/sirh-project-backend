package com.sirh.model.dto;

import com.sirh.model.entity.Pointeuse;

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
    public static PointeuseDto from(Pointeuse pointeuse) {
        return new PointeuseDto(
                pointeuse.getReference(),
                pointeuse.getNom(),
                pointeuse.getAdresseIp(),
                pointeuse.getPort(),
                pointeuse.getBatiment(),
                pointeuse.getStatutEnLigne(),
                pointeuse.getDerniereSynchro()
        );
    }
}