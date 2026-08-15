package com.sirh.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// Socle commun de toutes les entités JPA (sauf AuditLog, qui doit rester
// strictement immuable/append-only — cf. CLAUDE.md). `reference` est généré
// de manière différée (lazy) via @PrePersist plutôt qu'à l'instanciation,
// pour éviter une génération d'UUID inutile à chaque hydratation d'une ligne
// existante depuis la base (l'entité est alors immédiatement écrasée par
// Hibernate avec la vraie valeur lue en base).
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Column(updatable = false, nullable = false, unique = true)
    private UUID reference;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        if (this.reference == null) {
            this.reference = UUID.randomUUID();
        }
        // createdAt est géré par Spring Auditing (@CreatedDate)
    }

    @PreUpdate
    public void onUpdate() {
        // Hook préparé pour de futurs traitements (updatedAt est géré par @LastModifiedDate)
    }

    @PreRemove
    public void onDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
