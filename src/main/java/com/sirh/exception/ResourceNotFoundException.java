package com.sirh.exception;

// Entité introuvable — statut HTTP porté par l'ErrorCode (404 dans tous les
// cas actuels, mais ce n'est pas cette classe qui le décide).
public class ResourceNotFoundException extends SirhException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, Object[] args) {
        super(errorCode, args);
    }
}
