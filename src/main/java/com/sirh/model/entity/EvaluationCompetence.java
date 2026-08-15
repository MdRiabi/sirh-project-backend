package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "evaluation_competences")
@SQLDelete(sql = "UPDATE evaluation_competences SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCompetence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(nullable = false, length = 100)
    private String competence;

    // Nullable (V10 : ALTER COLUMN DROP NOT NULL) — une grille peut être
    // enregistrée incomplète ; seule la soumission (EvaluationService#mettreAJour)
    // bloque si une note manque encore (Module 9, cas exceptionnel).
    @Column(precision = 3, scale = 1)
    private BigDecimal note;

    @Column(name = "seuil_exigence", nullable = false, precision = 3, scale = 1)
    private BigDecimal seuilExigence;

    @Column(columnDefinition = "text")
    private String commentaire;
}
