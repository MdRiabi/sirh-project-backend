package com.sirh.fichiers.exception;

public class FichierNotFoundException extends RuntimeException {
    public FichierNotFoundException(Long id) {
        super("Fichier introuvable : " + id);
    }
}
