package com.sirh.exception;

// Violation de règle métier (statut invalide, doublon, quota dépassé,
// identifiants/jeton rejetés...) — statut HTTP porté par l'ErrorCode (409 le
// plus souvent, mais aussi 400/401/423 selon le cas : cette classe ne décrit
// qu'une "requête rejetée par l'état actuel du système", pas un code précis).
public class ConflictException extends SirhException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, Object[] args) {
        super(errorCode, args);
    }
}
