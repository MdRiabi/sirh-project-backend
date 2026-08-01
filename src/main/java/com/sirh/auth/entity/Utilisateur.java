package com.sirh.auth.entity;

import com.sirh.personnel.entity.Employe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false, unique = true)
    private Employe employe;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "compte_actif")
    private Boolean compteActif;

    @Column(name = "dernier_login")
    private LocalDateTime dernierLogin;

    @Column(name = "echecs_login_consecutifs")
    private Integer echecsLoginConsecutifs;

    @Column(name = "verrouille_jusqu_a")
    private LocalDateTime verrouilleJusquA;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}