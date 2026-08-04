package com.sirh.demandes.dto;

// bascule : type de remplacement optionnel (ex. CONGE_SANS_SOLDE) appliqué
// avant le calcul de déduction — permet au RH de convertir un congé payé en
// congé sans solde en cas de refus d'avance (cahier-fonctionnel.md Module 8).
// forcer : outrepasse un solde insuffisant (déduction quand même, motif
// FORCAGE_RH tracé dans historique_soldes).
public record IndisponibiliteValiderRequest(
        String bascule,
        Boolean forcer
) {
}
