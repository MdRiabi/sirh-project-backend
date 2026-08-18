package com.sirh.model.entity;

import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.enums.StatutBadge;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "evenements_360")
@SQLDelete(sql = "UPDATE evenements_360 SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvenementHistorique extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employe_id", nullable = false)
  private UUID employeId;

  @Column(nullable = false, length = 150)
  private String titre;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private EvenementCategorie categorie;

  @Enumerated(EnumType.STRING)
  @Column(name = "statut_badge", length = 20)
  @Builder.Default
  private StatutBadge statutBadge = StatutBadge.INFO;

  @Column(name = "saisi_par_id")
  private Long saisiParId;

  @Column(name = "date_evenement", nullable = false)
  private LocalDateTime dateEvenement;
}
