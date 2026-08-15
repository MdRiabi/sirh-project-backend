package com.sirh.exception;

// 409 — seule une demande EN_ATTENTE peut être traitée (approuvée/refusée).
public class DemandeDeverrouillageStatutInvalideException extends ConflictException {
    public DemandeDeverrouillageStatutInvalideException(String statutActuel) {
        super("Seule une demande EN_ATTENTE peut être traitée (statut actuel : " + statutActuel + ").");
    }
}
