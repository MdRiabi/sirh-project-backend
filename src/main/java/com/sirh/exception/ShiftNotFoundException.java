package com.sirh.exception;

import java.util.UUID;

public class ShiftNotFoundException extends RuntimeException {
    public ShiftNotFoundException(UUID reference) {
        super("Shift introuvable : " + reference);
    }

    // Conservé pour la résolution de la FK shift_id (Long, PK interne
    // inchangée) lors de la création d'un Planning — sans rapport avec
    // l'identifiant public de Shift (non exposé via un endpoint {reference}).
    public ShiftNotFoundException(Long shiftId) {
        super("Shift introuvable : " + shiftId);
    }
}