package com.sirh.model.dto;

import com.sirh.model.enums.ReportFormat;
import com.sirh.model.enums.ReportType;
import jakarta.validation.constraints.NotNull;

// POST /reports/generer — departementId optionnel (null = tous départements confondus).
public record ReportGenererRequest(
    @NotNull ReportType type,
    @NotNull ReportFormat format,
    @NotNull Integer annee,
    Long departementId) {}
