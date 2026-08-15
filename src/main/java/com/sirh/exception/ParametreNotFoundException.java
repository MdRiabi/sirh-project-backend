package com.sirh.exception;

public class ParametreNotFoundException extends ResourceNotFoundException {
    public ParametreNotFoundException(String cle) {
        super("Paramètre global introuvable : " + cle);
    }
}
