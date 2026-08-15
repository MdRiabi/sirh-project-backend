package com.sirh.model.repository;

import com.sirh.model.entity.Indisponibilite;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

/**
 * Module 11, "Faux Positif sur l'alerte de Sous-activité" (com.sirh.notifications,
 * SousActiviteService) : exclut du calcul les employés en suspension légale de
 * contrat active sur la fenêtre évaluée. Specifications plutôt qu'une requête
 * native (plan Lot 8) pour rester composable avec le reste du module Lot 5.
 */
public final class IndisponibiliteSpecifications {

    private IndisponibiliteSpecifications() {
    }

    public static Specification<Indisponibilite> suspensionLegaleActiveSur(LocalDate debutFenetre, LocalDate finFenetre,
                                                                             List<String> typesSuspensionLegale) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("statut"), "VALIDE"),
                root.get("typeIndisponibilite").in(typesSuspensionLegale),
                cb.lessThanOrEqualTo(root.get("dateDebut"), finFenetre),
                cb.greaterThanOrEqualTo(root.get("dateFin"), debutFenetre)
        );
    }
}
