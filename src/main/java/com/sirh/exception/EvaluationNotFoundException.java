package com.sirh.exception;

import java.util.UUID;

public class EvaluationNotFoundException extends ResourceNotFoundException {
    public EvaluationNotFoundException(UUID reference) {
        super("Évaluation introuvable : " + reference);
    }
}
