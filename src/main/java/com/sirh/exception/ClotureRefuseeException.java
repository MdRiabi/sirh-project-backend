package com.sirh.exception;

// 409 — préconditions de clôture non satisfaites (cahier-technique.md §4.7) :
// taux d'anomalies justifiées insuffisant, ou demande(s) encore EN_ATTENTE_RH
// sur le mois.
public class ClotureRefuseeException extends RuntimeException {
    public ClotureRefuseeException(String message) {
        super(message);
    }
}
