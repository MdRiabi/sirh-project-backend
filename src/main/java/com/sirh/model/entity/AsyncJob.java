package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import com.sirh.model.enums.AsyncJobStatut;
import com.sirh.model.enums.AsyncJobType;

@Entity
@Table(name = "async_jobs")
@SQLDelete(sql = "UPDATE async_jobs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AsyncJobType type;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AsyncJobStatut statut;

    @Column
    @Builder.Default
    private Integer progression = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String parametres;

    @Column(name = "resultat_url", length = 500)
    private String resultatUrl;

    @Column(name = "nombre_lignes_traitees")
    @Builder.Default
    private Integer nombreLignesTraitees = 0;

    @Column(name = "ligne_erreur")
    private Integer ligneErreur;

    @Column(name = "message_erreur", columnDefinition = "text")
    private String messageErreur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rapport_erreurs", columnDefinition = "jsonb")
    private String rapportErreurs;

    @Column(name = "execute_par_id")
    private Long executeParId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}