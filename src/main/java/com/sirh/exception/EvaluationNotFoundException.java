package com.sirh.exception;

import java.util.UUID;

public class EvaluationNotFoundException extends RuntimeException {
    public EvaluationNotFoundException(UUID reference) {
        super("Évaluation introuvable : " + reference);
    }
}
