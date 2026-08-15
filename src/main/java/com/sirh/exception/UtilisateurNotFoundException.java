package com.sirh.exception;

import java.util.UUID;

public class UtilisateurNotFoundException extends RuntimeException {
    public UtilisateurNotFoundException(UUID reference) {
        super("Utilisateur introuvable : " + reference);
    }
}