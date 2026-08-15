package com.sirh.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Variables de paie du mois pour un employé (Module 3, "traduire les
// présences en variables de paie") — pas un montant : aucun champ salaire
// n'existe sur employes, le calcul est destiné à l'export vers un logiciel de
// paie externe (cf. matricule_paie, ARCHITECTURE.md §1). heuresNormales +
// heuresMajorees = total travaillé ; heuresNuit est une étiquette
// indépendante (peut chevaucher l'une ou l'autre) utilisée pour une
// majoration de nuit calculée en aval.
public record CalculMensuelEmployeDto(
        UUID employeId,
        String nom,
        String prenom,
        BigDecimal heuresNormales,
        BigDecimal heuresNuit,
        BigDecimal heuresMajorees,
        BigDecimal tauxPresence,
        BigDecimal tauxPrime,
        int incidents,
        BigDecimal penaliteHeures) {
}
