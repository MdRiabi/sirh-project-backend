package com.sirh.exception;

public class UtilisateurNotFoundException extends RuntimeException {
    public UtilisateurNotFoundException(Long id) {
        super("Utilisateur introuvable : " + id);
    }
}