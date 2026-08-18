package com.sirh.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ParametreGlobalUpdateRequest(@NotBlank String valeur) {}
