package com.sirh.exception;

import java.util.UUID;

public class FichierNotFoundException extends ResourceNotFoundException {
    public FichierNotFoundException(UUID reference) {
        super("Fichier introuvable : " + reference);
    }
}
