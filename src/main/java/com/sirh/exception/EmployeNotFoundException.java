package com.sirh.exception;

import java.util.UUID;

public class EmployeNotFoundException extends ResourceNotFoundException {
    public EmployeNotFoundException(UUID id) {
        super("Employé introuvable : " + id);
    }

    public EmployeNotFoundException(String message) {
        super(message);
    }
}