package com.sirh.exception;

import java.util.UUID;

public class NotificationNotFoundException extends ResourceNotFoundException {
    public NotificationNotFoundException(UUID reference) {
        super("Notification introuvable : " + reference);
    }
}
