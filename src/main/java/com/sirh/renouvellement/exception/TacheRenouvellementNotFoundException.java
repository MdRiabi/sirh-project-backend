package com.sirh.renouvellement.exception;

public class TacheRenouvellementNotFoundException extends RuntimeException {
    public TacheRenouvellementNotFoundException(Long id) {
        super("Tâche de renouvellement introuvable : " + id);
    }
}
