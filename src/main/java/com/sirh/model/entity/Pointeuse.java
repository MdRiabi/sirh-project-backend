package com.sirh.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "pointeuses")
@SQLDelete(sql = "UPDATE pointeuses SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pointeuse extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nom;

  @Column(name = "adresse_ip", nullable = false, length = 45)
  private String adresseIp;

  @Column @Builder.Default private Integer port = 80;

  @Column(nullable = false, length = 50)
  private String batiment;

  @Column(name = "statut_en_ligne")
  @Builder.Default
  private Boolean statutEnLigne = true;

  @Column(name = "derniere_synchro")
  private LocalDateTime derniereSynchro;
}
