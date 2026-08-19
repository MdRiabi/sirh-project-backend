package com.sirh.model.entity;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.model.enums.FichierStatut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

  // Id technique renvoyé par le service s3-file-system (repo séparé) —
  // bucket/clé objet ne sont plus connus du SIRH, internes à ce service.
  // Nullable : absent tant que demanderUpload() n'a pas été appelé avec
  // succès (jamais le cas en pratique, la ligne est créée avec en même temps).
  @Column(name = "fichier_externe_id")
  private String fichierExterneId;

  @Column(name = "type_mime", nullable = false)
  private String typeMime;

  @Column(name = "taille_octets", nullable = false)
  private Long tailleOctets;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FichierCategorie categorie;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FichierStatut statut;

  @Enumerated(EnumType.STRING)
  @Column(name = "entite_type")
  private EntiteType entiteType;

  @Column(name = "entite_id")
  private Long entiteId;

  @Column(name = "uploade_par_id")
  private Long uploadeParId;
}
