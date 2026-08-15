package com.sirh.exception;

public class FichierNotFoundException extends RuntimeException {
    public FichierNotFoundException(Long id) {
        super("Fichier introuvable : " + id);
    }
}
