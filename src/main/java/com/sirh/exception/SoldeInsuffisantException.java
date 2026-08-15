package com.sirh.exception;

import java.math.BigDecimal;

// 409 — Module 8 : solde de congés payés insuffisant pour couvrir la demande.
// Le RH peut forcer la déduction (motif FORCAGE_RH, historique_soldes) ou
// basculer la demande en CONGE_SANS_SOLDE avant de revalider.
public class SoldeInsuffisantException extends RuntimeException {
    public SoldeInsuffisantException(BigDecimal soldeDisponible) {
        super(String.format("Solde insuffisant (%s jours disponibles)", soldeDisponible.stripTrailingZeros().toPlainString()));
    }
}
