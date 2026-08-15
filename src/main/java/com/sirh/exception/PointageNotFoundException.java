package com.sirh.exception;

import java.util.UUID;

public class PointageNotFoundException extends ResourceNotFoundException {
    public PointageNotFoundException(UUID reference) {
        super("Pointage introuvable : " + reference);
    }
}