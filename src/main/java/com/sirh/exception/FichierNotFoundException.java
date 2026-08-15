package com.sirh.exception;

import java.util.UUID;

public class FichierNotFoundException extends RuntimeException {
    public FichierNotFoundException(UUID reference) {
        super("Fichier introuvable : " + reference);
    }
}
