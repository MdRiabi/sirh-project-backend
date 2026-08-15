package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Clôture globale par mois (pas par département, cf. cahier-technique.md §3.24
// et décision d'architecture Lot 6) : une fois CLOTURE, pointages_bruts
// (correction manuelle) et autorisations/indisponibilites (création) sur ce
// mois sont refusés (409), cf. CloturePaieService.verifierMoisOuvert.
@Entity
@Table(name = "cloture_paie")
@SQLDelete(sql = "UPDATE cloture_paie SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloturePaie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate mois;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "OUVERT";

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "cloture_par_id")
    private UUID clotureParId;

    @Column(name = "taux_anomalies_justifiees")
    private BigDecimal tauxAnomaliesJustifiees;
}
