package com.sirh.assiduite.dto;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record ClotureRequest(@NotNull YearMonth mois) {
}
