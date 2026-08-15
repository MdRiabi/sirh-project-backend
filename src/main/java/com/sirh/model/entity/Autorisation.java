package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// Table complète (cahier-technique.md §3.10 — Module 13 : absences courtes,
// retards, sorties médicales). type reste une chaîne libre (ABSENCE_COURTE,
// SORTIE_MEDICALE, RETARD_JUSTIFIE, MISSION...), même convention que
// Indisponibilite.typeIndisponibilite — pas d'enum applicatif contraignant.
@Entity
@Table(name = "autorisations")
@SQLDelete(sql = "UPDATE autorisations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Autorisation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "date_autorisation", nullable = false)
    private LocalDate dateAutorisation;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(length = 30)
    @Builder.Default
    private String statut = "EN_ATTENTE_RH";

    @Column(name = "cree_par_id", nullable = false)
    private UUID creeParId;

    @Column(name = "cree_par_role", nullable = false, length = 20)
    private String creeParRole;

    @Column(name = "avis_manager_statut", length = 20)
    private String avisManagerStatut;

    @Column(name = "avis_manager_commentaire", columnDefinition = "TEXT")
    private String avisManagerCommentaire;

    @Column(name = "valide_par_rh_id")
    private UUID valideParRhId;
}
