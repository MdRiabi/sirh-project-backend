package com.sirh.outillages.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outillages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outillage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_barres", nullable = false, unique = true)
    private String codeBarres;

    @Column(name = "nom_outil", nullable = false)
    private String nomOutil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OutillageEtat etat = OutillageEtat.BON_ETAT;

    @Column(name = "attribue_a_employe_id")
    private UUID attribueAEmployeId;

    @Column(name = "date_attribution")
    private LocalDateTime dateAttribution;

    @Column(name = "departement_id", nullable = false)
    private Long departementId;

    @Column(name = "valeur_achat", nullable = false)
    private BigDecimal valeurAchat;

    @Column(name = "date_achat", nullable = false)
    private LocalDate dateAchat;
}
