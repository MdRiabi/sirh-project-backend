package com.sirh.reporting.dto;

import jakarta.validation.constraints.NotNull;

// POST /reports/generer — departementId optionnel (null = tous départements confondus).
public record ReportGenererRequest(
        @NotNull ReportType type,
        @NotNull ReportFormat format,
        @NotNull Integer annee,
        Long departementId
) {
}
