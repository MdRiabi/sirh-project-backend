package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;

@Entity
@Table(name = "pointages_bruts")
@SQLDelete(sql = "UPDATE pointages_bruts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageBrut extends BaseEntity {

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
}