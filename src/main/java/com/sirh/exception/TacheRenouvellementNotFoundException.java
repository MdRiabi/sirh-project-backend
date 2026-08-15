package com.sirh.exception;

import java.util.UUID;

public class TacheRenouvellementNotFoundException extends ResourceNotFoundException {
    public TacheRenouvellementNotFoundException(UUID reference) {
        super("Tâche de renouvellement introuvable : " + reference);
    }
}
