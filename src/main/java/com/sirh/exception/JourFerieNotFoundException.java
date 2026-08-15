package com.sirh.exception;

import java.util.UUID;

public class JourFerieNotFoundException extends ResourceNotFoundException {
    public JourFerieNotFoundException(UUID reference) {
        super("Jour férié introuvable : " + reference);
    }
}
