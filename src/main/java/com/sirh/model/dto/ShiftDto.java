package com.sirh.model.dto;

import com.sirh.model.entity.Shift;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftDto(
        UUID reference,
        String nom,
        String typeRotation,
        LocalTime heureDebut,
        LocalTime heureFin,
        LocalDateTime createdAt
) {
    public static ShiftDto from(Shift s) {
        return new ShiftDto(s.getReference(), s.getNom(), s.getTypeRotation(), s.getHeureDebut(), s.getHeureFin(), s.getCreatedAt());
    }
}