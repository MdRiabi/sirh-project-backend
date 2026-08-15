package com.sirh.exception;

public class IndisponibiliteNotFoundException extends RuntimeException {
    public IndisponibiliteNotFoundException(Long id) {
        super("Indisponibilité introuvable : " + id);
    }
}
