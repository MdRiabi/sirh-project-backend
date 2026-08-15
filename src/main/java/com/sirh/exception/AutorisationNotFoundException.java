package com.sirh.exception;

import java.util.UUID;

public class AutorisationNotFoundException extends ResourceNotFoundException {
    public AutorisationNotFoundException(UUID reference) {
        super("Autorisation introuvable : " + reference);
    }
}
