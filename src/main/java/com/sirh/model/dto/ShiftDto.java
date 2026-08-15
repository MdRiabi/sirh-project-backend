package com.sirh.model.dto;

import com.sirh.model.entity.Shift;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ShiftDto(
        Long id,
        String nom,
        String typeRotation,
        LocalTime heureDebut,
        LocalTime heureFin,
        LocalDateTime createdAt
) {
    public static ShiftDto from(Shift s) {
        return new ShiftDto(s.getId(), s.getNom(), s.getTypeRotation(), s.getHeureDebut(), s.getHeureFin(), s.getCreatedAt());
    }
}