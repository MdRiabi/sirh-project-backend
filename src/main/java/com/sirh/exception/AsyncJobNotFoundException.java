package com.sirh.exception;

import java.util.UUID;

public class AsyncJobNotFoundException extends ResourceNotFoundException {
    public AsyncJobNotFoundException(UUID reference) {
        super("Job asynchrone introuvable : " + reference);
    }
}