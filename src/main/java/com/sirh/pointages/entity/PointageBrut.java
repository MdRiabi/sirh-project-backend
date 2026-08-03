package com.sirh.pointages.entity;

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
@Table(name = "pointages_bruts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageBrut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(name = "pointeuse_id")
    private Long pointeuseId;

    @Column(name = "timestamp_pointage", nullable = false)
    private LocalDateTime timestampPointage;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 20)
    private TypeEvenement typeEvenement;

    @Enumerated(EnumType.STRING)
    @Column(name = "methode_validation", nullable = false, length = 30)
    private MethodeValidation methodeValidation;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_import", nullable = false, length = 20)
    private SourceImport sourceImport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PointageStatut statut = PointageStatut.OK;

    @Column(name = "autorisation_id")
    private Long autorisationId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}