package com.sirh.exception;

public class PointeuseNotFoundException extends RuntimeException {
    public PointeuseNotFoundException(Long id) {
        super("Pointeuse introuvable : " + id);
    }

    public PointeuseNotFoundException(String message) {
        super(message);
    }
}