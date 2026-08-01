package com.sirh.fichiers.entity;

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
@Table(name = "fichiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fichier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_original", nullable = false)
    private String nomOriginal;

    @Column(name = "nom_stockage", nullable = false, unique = true)
    private String nomStockage;

    @Column(name = "chemin_relatif", nullable = false)
    private String cheminRelatif;

    @Column(name = "type_mime", nullable = false)
    private String typeMime;

    @Column(name = "taille_octets", nullable = false)
    private Long tailleOctets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FichierCategorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "entite_type")
    private EntiteType entiteType;

    @Column(name = "entite_id")
    private Long entiteId;

    @Column(name = "uploade_par_id")
    private Long uploadeParId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
