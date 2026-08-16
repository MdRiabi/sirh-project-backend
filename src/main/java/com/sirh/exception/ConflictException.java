package com.sirh.exception;

// Parent commun des violations de règle métier (statut invalide, doublon,
// contrainte, quota dépassé...) -> HTTP 409 (cf. GlobalExceptionHandler).
public class ConflictException extends SirhException {
    public ConflictException(String message) {
        super(message);
    }
}
