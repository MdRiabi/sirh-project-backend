package com.sirh.personnel.entity;

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

/**
 * Version minimale requise par le socle Auth (Lot 0) — CRUD complet et
 * relation vers Departement en Lot 1 (cf. docs/BACKLOG.md).
 */
@Entity
@Table(name = "employes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "matricule_interne", nullable = false, unique = true)
    private String matriculeInterne;

    @Column(name = "matricule_cnss", unique = true)
    private String matriculeCnss;

    @Column(name = "matricule_paie", unique = true)
    private String matriculePaie;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true)
    private String email;

    @Column(name = "departement_id")
    private Long departementId;

    @Column(name = "type_contrat", nullable = false)
    private String typeContrat;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Column(name = "solde_conges_jours")
    private BigDecimal soldeCongesJours;

    private Boolean actif;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}