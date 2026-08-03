package com.sirh.pointages.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pointeuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pointeuse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(name = "adresse_ip", nullable = false, unique = true, length = 45)
    private String adresseIp;

    @Column
    @Builder.Default
    private Integer port = 80;

    @Column(nullable = false, length = 50)
    private String batiment;

    @Column(name = "statut_en_ligne")
    @Builder.Default
    private Boolean statutEnLigne = true;

    @Column(name = "derniere_synchro")
    private LocalDateTime derniereSynchro;
}