package com.sirh.auth.exception;

import java.time.LocalDateTime;

public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Compte verrouillé jusqu'à " + lockedUntil + " suite à 5 échecs de connexion consécutifs.");
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
