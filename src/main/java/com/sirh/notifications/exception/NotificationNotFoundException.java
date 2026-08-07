package com.sirh.notifications.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("Notification introuvable : " + id);
    }
}
