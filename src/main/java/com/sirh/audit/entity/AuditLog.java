package com.sirh.audit.entity;

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

/**
 * Immuable : écriture uniquement via AuditAspect, aucune mise à jour/suppression
 * applicative. Lecture réservée SUPERUSER (cf. ARCHITECTURE.md §9) — l'endpoint
 * de consultation arrive en Lot 11.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "table_cible", nullable = false, length = 50)
    private String tableCible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anciennes_valeurs", columnDefinition = "jsonb")
    private String anciennesValeurs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelles_valeurs", columnDefinition = "jsonb")
    private String nouvellesValeurs;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
