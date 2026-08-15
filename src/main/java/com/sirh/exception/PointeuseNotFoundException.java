package com.sirh.exception;

import java.util.UUID;

public class PointeuseNotFoundException extends RuntimeException {
    public PointeuseNotFoundException(UUID reference) {
        super("Pointeuse introuvable : " + reference);
    }

    public PointeuseNotFoundException(String message) {
        super(message);
    }
}