package com.sirh.auth.dto;

import java.util.UUID;

public record UserSummary(
        Long userId,
        UUID employeId,
        String username,
        String nom,
        String prenom,
        String role,
        Long departementId
) {
}
