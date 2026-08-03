package com.sirh.pointages.exception;

public class PointageNotFoundException extends RuntimeException {
    public PointageNotFoundException(Long id) {
        super("Pointage introuvable : " + id);
    }
}