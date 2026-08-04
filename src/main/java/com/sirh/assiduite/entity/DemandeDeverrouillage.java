package com.sirh.assiduite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Workflow de déverrouillage exceptionnel d'un mois clôturé
// (cahier-fonctionnel.md Module 3, cas exceptionnel) : motif légal obligatoire,
// transmis au RH pour décision.
@Entity
@Table(name = "demandes_deverrouillage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeDeverrouillage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate mois;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(name = "demande_par_id", nullable = false)
    private UUID demandeParId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "EN_ATTENTE";

    @Column(name = "traite_par_id")
    private UUID traiteParId;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
