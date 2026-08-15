package com.sirh.model.repository;

import com.sirh.model.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

// Filtres optionnels de GET /audit-logs (Lot 11) — même style que
// IndisponibiliteSpecifications (Lot 5) : chaque critère n'est appliqué que
// s'il est fourni, combinés en AND.
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> avecFiltres(String action, String tableCible, Long utilisateurId,
                                                        LocalDateTime dateDebut, LocalDateTime dateFin) {
        return (root, query, cb) -> {
            var predicats = cb.conjunction();
            if (action != null && !action.isBlank()) {
                predicats = cb.and(predicats, cb.equal(root.get("action"), action));
            }
            if (tableCible != null && !tableCible.isBlank()) {
                predicats = cb.and(predicats, cb.equal(root.get("tableCible"), tableCible));
            }
            if (utilisateurId != null) {
                predicats = cb.and(predicats, cb.equal(root.get("utilisateurId"), utilisateurId));
            }
            if (dateDebut != null) {
                predicats = cb.and(predicats, cb.greaterThanOrEqualTo(root.get("createdAt"), dateDebut));
            }
            if (dateFin != null) {
                predicats = cb.and(predicats, cb.lessThanOrEqualTo(root.get("createdAt"), dateFin));
            }
            return predicats;
        };
    }
}
