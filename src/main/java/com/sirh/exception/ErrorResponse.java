package com.sirh.exception;

import java.time.LocalDateTime;

// Corps JSON unique retourné par GlobalExceptionHandler pour toute erreur de
// l'API — le frontend n'a qu'une seule structure à parser, quel que soit le
// type d'exception levé côté backend.
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path);
    }
}
