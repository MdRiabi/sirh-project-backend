package com.sirh.exception;

import java.util.UUID;

public class IndisponibiliteNotFoundException extends RuntimeException {
    public IndisponibiliteNotFoundException(UUID reference) {
        super("Indisponibilité introuvable : " + reference);
    }
}
