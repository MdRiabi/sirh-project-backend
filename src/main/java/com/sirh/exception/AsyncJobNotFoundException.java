package com.sirh.exception;

public class AsyncJobNotFoundException extends RuntimeException {
    public AsyncJobNotFoundException(Long id) {
        super("Job asynchrone introuvable : " + id);
    }
}