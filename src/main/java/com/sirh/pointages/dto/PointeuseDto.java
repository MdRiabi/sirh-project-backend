package com.sirh.pointages.dto;

import com.sirh.pointages.entity.Pointeuse;

import java.time.LocalDateTime;

public record PointeuseDto(
        Long id,
        String nom,
        String adresseIp,
        Integer port,
        String batiment,
        Boolean statutEnLigne,
        LocalDateTime derniereSynchro
) {
    public static PointeuseDto from(Pointeuse pointeuse) {
        return new PointeuseDto(
                pointeuse.getId(),
                pointeuse.getNom(),
                pointeuse.getAdresseIp(),
                pointeuse.getPort(),
                pointeuse.getBatiment(),
                pointeuse.getStatutEnLigne(),
                pointeuse.getDerniereSynchro()
        );
    }
}