package com.sirh.exception;

// 409 — check-out sur un outil déjà attribué, ou check-in sur un outil non
// attribué (cahier-fonctionnel.md Module 5, workflow rattachement/restitution).
public class OutillageMouvementInvalideException extends RuntimeException {
    public OutillageMouvementInvalideException(String message) {
        super(message);
    }
}
