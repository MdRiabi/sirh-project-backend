package com.sirh.evaluation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans_developpement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDeveloppement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(nullable = false, length = 100)
    private String competence;

    @Column(columnDefinition = "text", nullable = false)
    private String actions;

    @Generated(event = EventType.INSERT)
    @Column(name = "date_creation", insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Pas de transition de statut dans ce lot (aucun endpoint ne la modifie,
    // cf. cahier-technique.md §4.10) : simple chaîne libre, valeur par défaut
    // en base ('A_TRAITER').
    @Column(length = 20)
    private String statut;
}
