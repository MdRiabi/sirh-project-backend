package com.sirh.personnel.exception;

public class DepartementNotFoundException extends RuntimeException {
    public DepartementNotFoundException(Long id) {
        super("Département introuvable : " + id);
    }
}
