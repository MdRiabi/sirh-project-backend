package com.sirh.renouvellement.exception;

// 409 — tentative de traiter une tâche déjà VALIDE ou NON_RENOUVELE
// (URGENT_EXPIRE reste actionnable : cf. cahier-fonctionnel.md Module 12,
// "le contrat n'est pas clôturé automatiquement pour éviter de bloquer la paie").
public class TacheRenouvellementStatutInvalideException extends RuntimeException {
    public TacheRenouvellementStatutInvalideException(String message) {
        super(message);
    }
}
