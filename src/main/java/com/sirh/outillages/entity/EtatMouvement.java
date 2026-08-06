package com.sirh.outillages.entity;

// Vocabulaire exact du schéma (cahier-technique.md §3.14) : état déclaré par
// le Manager à la restitution (CHECK_IN uniquement, null pour un CHECK_OUT).
public enum EtatMouvement {
    BON,
    USE,
    CASSE,
    PERDU
}
