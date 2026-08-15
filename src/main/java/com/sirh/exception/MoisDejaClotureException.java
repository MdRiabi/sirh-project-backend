package com.sirh.exception;

import java.time.LocalDate;

// 409 — utilisée à la fois pour un POST /assiduite/cloturer-paie sur un mois
// déjà CLOTURE et pour le verrou d'écriture (pointages_bruts, autorisations,
// indisponibilites) appliqué par les autres modules via
// CloturePaieService.verifierMoisOuvert (cahier-fonctionnel.md Module 3, cas
// exceptionnel "tentative de modification sur un mois clôturé").
public class MoisDejaClotureException extends RuntimeException {
    public MoisDejaClotureException(LocalDate mois) {
        super("Le mois " + mois.getYear() + "-" + String.format("%02d", mois.getMonthValue())
                + " est clôturé (lecture seule). Utilisez /assiduite/demandes-deverrouillage pour un déverrouillage exceptionnel.");
    }
}
