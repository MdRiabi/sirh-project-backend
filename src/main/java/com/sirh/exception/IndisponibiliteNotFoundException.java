package com.sirh.exception;

import java.util.UUID;

public class IndisponibiliteNotFoundException extends ResourceNotFoundException {
    public IndisponibiliteNotFoundException(UUID reference) {
        super("Indisponibilité introuvable : " + reference);
    }
}
