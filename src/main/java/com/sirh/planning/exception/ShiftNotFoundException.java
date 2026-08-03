package com.sirh.planning.exception;

public class ShiftNotFoundException extends RuntimeException {
    public ShiftNotFoundException(Long id) {
        super("Shift introuvable : " + id);
    }
}