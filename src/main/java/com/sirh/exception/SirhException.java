package com.sirh.exception;

// Racine commune de toutes les exceptions métier du projet — porte l'ErrorCode
// (statut HTTP + clé de traduction messages.properties) et les arguments
// positionnels du message ({0}, {1}...), jamais un message déjà formaté :
// la résolution i18n se fait au bord (GlobalExceptionHandler, ou tout autre
// point qui a besoin du texte final) via MessageSource, pas ici.
public abstract class SirhException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    protected SirhException(ErrorCode errorCode) {
        this(errorCode, new Object[0], null);
    }

    protected SirhException(ErrorCode errorCode, Object[] args) {
        this(errorCode, args, null);
    }

    protected SirhException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode.name(), cause);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
