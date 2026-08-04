package com.sirh.assiduite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record DemandeDeverrouillageCreateRequest(@NotNull YearMonth mois, @NotBlank String motif) {
}
