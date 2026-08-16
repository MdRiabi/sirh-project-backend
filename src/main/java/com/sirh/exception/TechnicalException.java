package com.sirh.exception;

// Parent commun des erreurs système/infrastructure (I/O, stockage, appel
// externe...) -> HTTP 500 (cf. GlobalExceptionHandler).
public class TechnicalException extends SirhException {
    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
