package com.sirh.exception;

// Racine commune de toutes les exceptions métier du projet — permet au
// GlobalExceptionHandler de traiter globalement ResourceNotFoundException/
// ConflictException/TechnicalException sans dupliquer un @ExceptionHandler
// par exception concrète.
public abstract class SirhException extends RuntimeException {

    protected SirhException(String message) {
        super(message);
    }

    protected SirhException(String message, Throwable cause) {
        super(message, cause);
    }
}
