package com.sirh.exception;

import java.util.UUID;

public class OutillageNotFoundException extends RuntimeException {
    public OutillageNotFoundException(UUID reference) {
        super("Outil introuvable : " + reference);
    }

    // Conservé pour la résolution interne par PK (Long) dans OutillageService
    // (checkOut/checkIn/historique, appelée après résolution de la référence
    // publique par le Controller) — sans rapport avec l'identifiant exposé
    // par l'API (GET/POST /outillages/{reference}).
    public OutillageNotFoundException(Long id) {
        super("Outil introuvable : " + id);
    }
}
