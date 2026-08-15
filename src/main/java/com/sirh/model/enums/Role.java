package com.sirh.model.enums;

/**
 * Les 5 rôles métier (cf. cahier-technique.md §2). L'EMPLOYE n'a accès
 * qu'en lecture seule à son propre planning — aucun autre droit.
 */
public enum Role {
    ROLE_EMPLOYE,
    ROLE_MANAGER,
    ROLE_RH,
    ROLE_DIRECTEUR,
    ROLE_SUPERUSER
}