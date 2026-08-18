package com.sirh.model.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

// Table générique cle/valeur (cahier-technique.md §3.24) : évite de coder en
// dur une règle métier susceptible de changer sans redéploiement (ex. taux
// d'incrémentation mensuelle des congés, Module 8). Réutilisable par
// d'autres lots sans nouvelle migration à chaque nouveau paramètre.
@Entity
@Table(name = "parametres_globaux")
@SQLDelete(sql = "UPDATE parametres_globaux SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreGlobal extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String cle;

  @Column(nullable = false, length = 255)
  private String valeur;

  @Column(nullable = false, length = 20)
  @Builder.Default
  private String type = "DECIMAL";

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "modifie_par_id")
  private UUID modifieParId;
}
