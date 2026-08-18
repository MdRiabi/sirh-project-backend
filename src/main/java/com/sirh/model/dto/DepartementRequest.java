package com.sirh.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DepartementRequest(@NotBlank String nom, UUID managerId) {}
