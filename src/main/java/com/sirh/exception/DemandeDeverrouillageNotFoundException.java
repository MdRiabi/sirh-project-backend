package com.sirh.exception;

import java.util.UUID;

public class DemandeDeverrouillageNotFoundException extends RuntimeException {
    public DemandeDeverrouillageNotFoundException(UUID reference) {
        super("Demande de déverrouillage introuvable : " + reference);
    }
}
