package com.sirh.personnel.exception;

import java.util.UUID;

public class EmployeNotFoundException extends RuntimeException {
    public EmployeNotFoundException(UUID id) {
        super("Employé introuvable : " + id);
    }
}