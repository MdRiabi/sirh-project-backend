package com.sirh.exception;

// Parent commun des exceptions "entité introuvable" -> HTTP 404 (cf.
// GlobalExceptionHandler). Chaque *NotFoundException garde son propre nom/
// message métier, seule la classification change.
public class ResourceNotFoundException extends SirhException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
