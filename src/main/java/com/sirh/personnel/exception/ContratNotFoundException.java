package com.sirh.personnel.exception;

public class ContratNotFoundException extends RuntimeException {
    public ContratNotFoundException(Long id) {
        super("Contrat introuvable : " + id);
    }
}