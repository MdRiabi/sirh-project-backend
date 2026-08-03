package com.sirh.async.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "async_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncJob {

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

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}