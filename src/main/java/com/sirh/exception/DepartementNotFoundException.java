package com.sirh.exception;

import java.util.UUID;

public class DepartementNotFoundException extends RuntimeException {
    public DepartementNotFoundException(UUID reference) {
        super("Département introuvable : " + reference);
    }

    // Conservé pour la résolution de la FK departement_id (Long, PK interne
    // inchangée) lors du rattachement d'un Employe à son Departement — sans
    // rapport avec l'identifiant public exposé par GET /departements/{reference}.
    public DepartementNotFoundException(Long departementId) {
        super("Département introuvable : " + departementId);
    }
}
