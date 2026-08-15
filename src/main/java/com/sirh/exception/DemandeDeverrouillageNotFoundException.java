package com.sirh.exception;

import java.util.UUID;

public class DemandeDeverrouillageNotFoundException extends ResourceNotFoundException {
    public DemandeDeverrouillageNotFoundException(UUID reference) {
        super("Demande de déverrouillage introuvable : " + reference);
    }
}
