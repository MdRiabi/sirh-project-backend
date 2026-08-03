package com.sirh.pointages.dto;

import com.sirh.pointages.entity.TypeEvenement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CorrectionPointageRequest(
        @NotNull LocalDateTime timestampPointage,
        @NotNull TypeEvenement typeEvenement,
        @NotBlank String motif
) {
}