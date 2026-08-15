package com.sirh.exception;

import java.util.UUID;

public class ContratNotFoundException extends ResourceNotFoundException {
    public ContratNotFoundException(UUID reference) {
        super("Contrat introuvable : " + reference);
    }
}