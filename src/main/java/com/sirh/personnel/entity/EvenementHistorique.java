package com.sirh.personnel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evenements_360")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvenementHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EvenementCategorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_badge", length = 20)
    @Builder.Default
    private StatutBadge statutBadge = StatutBadge.INFO;

    @Column(name = "saisi_par_id")
    private Long saisiParId;

    @Column(name = "date_evenement", nullable = false)
    private LocalDateTime dateEvenement;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}