package com.sirh.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;

@Entity
@Table(name = "fichiers")
@SQLDelete(sql = "UPDATE fichiers SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fichier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_original", nullable = false)
    private String nomOriginal;

    @Column(name = "nom_stockage", nullable = false)
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
}
