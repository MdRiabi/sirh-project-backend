package com.sirh.exception;

// Erreur système/infrastructure (I/O, stockage, invariant technique violé...)
// -> 500. Le message affiché au client reste générique (cf.
// messages.properties, SYSTEM_ERROR) : la cause réelle est loggée côté
// serveur par l'appelant, jamais renvoyée au front (pas de stack trace exposée).
public class TechnicalException extends SirhException {

    public TechnicalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TechnicalException(ErrorCode errorCode, Object[] args) {
        super(errorCode, args);
    }

    public TechnicalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, new Object[0], cause);
    }

    public TechnicalException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode, args, cause);
    }
}
