package com.sirh.outillages.exception;

public class OutillageNotFoundException extends RuntimeException {
    public OutillageNotFoundException(Long id) {
        super("Outil introuvable : " + id);
    }
}
