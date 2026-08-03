package com.sirh.calendrier.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jours_feries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeJourFerie type;

    @Builder.Default
    private Boolean national = true;

    @Column(length = 50)
    private String region;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
