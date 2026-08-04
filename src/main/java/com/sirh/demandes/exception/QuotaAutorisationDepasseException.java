package com.sirh.demandes.exception;

// 409 — Module 13 : au-delà de 4h cumulées sur le mois, la validation RH doit
// explicitement préciser le traitement des heures excédentaires (déduction
// salariale ou récupération) avant de pouvoir valider.
public class QuotaAutorisationDepasseException extends RuntimeException {
    public QuotaAutorisationDepasseException(int depassementMinutes) {
        super(String.format(
                "[QUOTA DÉPASSÉ : +%dmin] Précisez le traitement des heures excédentaires (traitementDepassement : DEDUCTION_SALAIRE ou RECUPERATION) pour valider.",
                depassementMinutes));
    }
}
