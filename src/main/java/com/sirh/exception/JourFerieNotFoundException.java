package com.sirh.exception;

import java.util.UUID;

public class JourFerieNotFoundException extends RuntimeException {
    public JourFerieNotFoundException(UUID reference) {
        super("Jour férié introuvable : " + reference);
    }
}
