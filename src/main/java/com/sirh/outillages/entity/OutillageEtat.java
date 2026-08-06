package com.sirh.outillages.entity;

// Statut courant de l'outil (colonne outillages.etat) — distinct du vocabulaire
// de EtatMouvement déclaré à la restitution (cahier-technique.md §3.14) :
// BON_ETAT est la valeur par défaut/disponible (schéma V1), ATTRIBUE le
// marque en cours de check-out ; USE/CASSE/PERDU reflètent le dernier état
// déclaré au check-in (cf. OutillageService.appliquerEtatApresCheckIn).
public enum OutillageEtat {
    BON_ETAT,
    ATTRIBUE,
    USE,
    CASSE,
    PERDU
}
