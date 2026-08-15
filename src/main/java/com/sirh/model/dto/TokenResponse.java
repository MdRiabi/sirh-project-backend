package com.sirh.model.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserSummary user
) {
}
