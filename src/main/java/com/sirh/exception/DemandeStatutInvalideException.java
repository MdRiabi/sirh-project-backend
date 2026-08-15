package com.sirh.exception;

// 409 — tentative de valider/refuser une autorisation ou indisponibilité qui
// n'est plus (ou pas encore) EN_ATTENTE_RH.
public class DemandeStatutInvalideException extends ConflictException {
    public DemandeStatutInvalideException(String message) {
        super(message);
    }
}
