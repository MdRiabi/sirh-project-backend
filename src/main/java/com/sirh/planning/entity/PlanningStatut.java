package com.sirh.planning.entity;

public enum PlanningStatut {
    PROVISOIRE,
    VALIDE,
    REPOS,
    // Générés/écrasés automatiquement à la validation d'une demande (Lot 5,
    // com.sirh.demandes) : CONGE pour une indisponibilité de type congé
    // (payé/sans solde), ABSENCE pour les autres cas (autorisation d'absence
    // courte, indisponibilité non-congé type maladie).
    CONGE,
    ABSENCE
}