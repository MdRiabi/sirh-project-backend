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
import java.util.UUID;

// Table complète (cahier-technique.md §3.11) — seule la lecture (chevauchement
// planning/congé validé, Lot 3) est utilisée ici ; le CRUD (saisie Manager/RH,
// workflow d'avis + validation) est le périmètre du Lot 5.
@Entity
@Table(name = "indisponibilites")
@SQLDelete(sql = "UPDATE indisponibilites SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Indisponibilite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(name = "type_indisponibilite", nullable = false, length = 30)
    private String typeIndisponibilite;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(length = 30)
    @Builder.Default
    private String statut = "EN_ATTENTE_RH";

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "piece_jointe_id")
    private Long pieceJointeId;

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