package com.sirh.exception;

public class AutorisationNotFoundException extends RuntimeException {
    public AutorisationNotFoundException(Long id) {
        super("Autorisation introuvable : " + id);
    }
}
