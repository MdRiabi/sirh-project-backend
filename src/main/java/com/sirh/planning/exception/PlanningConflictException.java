package com.sirh.planning.exception;

// 409 — violation du repos légal de 11h, chevauchement direct avec une autre
// affectation, ou chevauchement avec un congé validé (cahier-fonctionnel.md
// Module 2, "Cas Exceptionnels").
public class PlanningConflictException extends RuntimeException {
    public PlanningConflictException(String message) {
        super(message);
    }
}