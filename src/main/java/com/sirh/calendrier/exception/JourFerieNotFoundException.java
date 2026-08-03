package com.sirh.calendrier.exception;

public class JourFerieNotFoundException extends RuntimeException {
    public JourFerieNotFoundException(Long id) {
        super("Jour férié introuvable : " + id);
    }
}
