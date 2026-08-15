package com.sirh.exception;

public class ParametreNotFoundException extends RuntimeException {
    public ParametreNotFoundException(String cle) {
        super("Paramètre global introuvable : " + cle);
    }
}
