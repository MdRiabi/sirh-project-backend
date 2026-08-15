package com.sirh.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID reference) {
        super("Notification introuvable : " + reference);
    }
}
