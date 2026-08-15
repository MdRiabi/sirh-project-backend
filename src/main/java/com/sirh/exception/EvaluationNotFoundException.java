package com.sirh.exception;

public class EvaluationNotFoundException extends RuntimeException {
    public EvaluationNotFoundException(Long id) {
        super("Évaluation introuvable : " + id);
    }
}
