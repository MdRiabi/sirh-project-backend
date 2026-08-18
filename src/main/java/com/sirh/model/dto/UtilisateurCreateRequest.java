package com.sirh.model.dto;

import com.sirh.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UtilisateurCreateRequest(
    @NotNull UUID employeId,
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password,
    @NotNull Role role) {}
