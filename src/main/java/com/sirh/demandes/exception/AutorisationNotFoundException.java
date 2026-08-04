package com.sirh.demandes.exception;

public class AutorisationNotFoundException extends RuntimeException {
    public AutorisationNotFoundException(Long id) {
        super("Autorisation introuvable : " + id);
    }
}
