package com.sirh.assiduite.exception;

public class DemandeDeverrouillageNotFoundException extends RuntimeException {
    public DemandeDeverrouillageNotFoundException(Long id) {
        super("Demande de déverrouillage introuvable : " + id);
    }
}
