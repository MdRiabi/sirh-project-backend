package com.sirh.demandes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Table complète (cahier-technique.md §3.12). Le motif VALIDATION_CONGE /
// FORCAGE_RH / INCREMENT_MENSUEL relève du périmètre Lot 5/Lot 6 ; seul
// RECREDIT_FERIE (Lot 4, chevauchement férié/congé validé) écrit ici pour
// l'instant.
@Entity
@Table(name = "historique_soldes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueSolde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDate dateMouvement;

    @Column(name = "delta_jours", nullable = false, precision = 5, scale = 2)
    private BigDecimal deltaJours;

    @Column(name = "solde_apres", nullable = false, precision = 5, scale = 2)
    private BigDecimal soldeApres;

    @Column(nullable = false, length = 50)
    private String motif;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
