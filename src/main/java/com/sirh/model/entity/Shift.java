package com.sirh.model.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "shifts")
@SQLDelete(sql = "UPDATE shifts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String nom;

  // FIXE, 2X8, 3X8 (cahier-technique.md §3.8) — chaîne libre, pas d'enum Java
  // car "2X8"/"3X8" ne sont pas des identifiants valides.
  @Column(name = "type_rotation", nullable = false, length = 20)
  private String typeRotation;

  @Column(name = "heure_debut", nullable = false)
  private LocalTime heureDebut;

  @Column(name = "heure_fin", nullable = false)
  private LocalTime heureFin;
}
