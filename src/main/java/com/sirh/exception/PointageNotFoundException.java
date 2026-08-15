package com.sirh.exception;

import java.util.UUID;

public class PointageNotFoundException extends RuntimeException {
    public PointageNotFoundException(UUID reference) {
        super("Pointage introuvable : " + reference);
    }
}