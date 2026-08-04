package com.sirh.assiduite.exception;

// 409 — seule une demande EN_ATTENTE peut être traitée (approuvée/refusée).
public class DemandeDeverrouillageStatutInvalideException extends RuntimeException {
    public DemandeDeverrouillageStatutInvalideException(String statutActuel) {
        super("Seule une demande EN_ATTENTE peut être traitée (statut actuel : " + statutActuel + ").");
    }
}
