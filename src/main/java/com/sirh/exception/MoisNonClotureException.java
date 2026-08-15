package com.sirh.exception;

// 400 — une demande de déverrouillage exceptionnel n'a de sens que pour un
// mois déjà CLOTURE.
public class MoisNonClotureException extends RuntimeException {
    public MoisNonClotureException() {
        super("Ce mois n'est pas clôturé — aucun déverrouillage à demander.");
    }
}
