package com.sirh.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "plans_developpement")
@SQLDelete(sql = "UPDATE plans_developpement SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDeveloppement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "evaluation_id", nullable = false)
  private Long evaluationId;

  @Column(nullable = false, length = 100)
  private String competence;

  @Column(columnDefinition = "text", nullable = false)
  private String actions;

  // Pas de transition de statut dans ce lot (aucun endpoint ne la modifie,
  // cf. cahier-technique.md §4.10) : simple chaîne libre, valeur par défaut
  // en base ('A_TRAITER').
  @Column(length = 20)
  private String statut;
}
