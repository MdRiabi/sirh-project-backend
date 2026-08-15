package com.sirh.exception;

import java.util.UUID;

public class UtilisateurNotFoundException extends ResourceNotFoundException {
    public UtilisateurNotFoundException(UUID reference) {
        super("Utilisateur introuvable : " + reference);
    }
}