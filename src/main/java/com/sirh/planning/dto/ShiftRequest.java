package com.sirh.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ShiftRequest(
        @NotBlank String nom,
        @NotBlank String typeRotation,
        @NotNull LocalTime heureDebut,
        @NotNull LocalTime heureFin
) {
}