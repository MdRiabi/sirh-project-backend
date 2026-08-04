package com.sirh.demandes.exception;

// 409 — Module 13 : un Manager ne peut saisir une régularisation que sous 72h
// suivant l'absence à justifier ; seul le RH peut passer outre (workflow
// d'urgence, cree_par_role = RH, jamais soumis à ce contrôle).
public class DelaiRegularisationDepasseException extends RuntimeException {
    public DelaiRegularisationDepasseException() {
        super("[DÉLAI DÉPASSÉ] Les régularisations doivent être effectuées sous 72h. Seul le RH peut outrepasser ce délai.");
    }
}
