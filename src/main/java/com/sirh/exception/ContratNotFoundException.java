package com.sirh.exception;

public class ContratNotFoundException extends RuntimeException {
    public ContratNotFoundException(Long id) {
        super("Contrat introuvable : " + id);
    }
}