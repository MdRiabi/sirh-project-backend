package com.sirh.exception;

import java.util.UUID;

public class AutorisationNotFoundException extends RuntimeException {
    public AutorisationNotFoundException(UUID reference) {
        super("Autorisation introuvable : " + reference);
    }
}
