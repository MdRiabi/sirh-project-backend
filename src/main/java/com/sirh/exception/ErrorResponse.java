package com.sirh.exception;

import java.time.LocalDateTime;

// Corps JSON unique retourné par GlobalExceptionHandler pour toute erreur de
// l'API — "code" est la clé stable consommée par le frontend (i18n côté
// client, routage vers un message dédié...), "message" reste la traduction
// serveur déjà résolue (fallback d'affichage direct).
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, code, message, path);
    }
}
