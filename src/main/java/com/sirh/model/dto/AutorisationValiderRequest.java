package com.sirh.model.dto;

// traitementDepassement : DEDUCTION_SALAIRE | RECUPERATION — requis
// uniquement si le cumul mensuel de l'employé dépasse le quota de 4h
// (cahier-fonctionnel.md Module 13). Simple décision tracée dans audit_logs,
// pas de colonne dédiée sur autorisations.
public record AutorisationValiderRequest(
        String traitementDepassement
) {
}
