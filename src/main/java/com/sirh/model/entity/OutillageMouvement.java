package com.sirh.model.entity;

import com.sirh.model.enums.EtatMouvement;
import com.sirh.model.enums.TypeMouvement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "outillage_mouvements")
@SQLDelete(sql = "UPDATE outillage_mouvements SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutillageMouvement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "outillage_id", nullable = false)
  private Long outillageId;

  @Column(name = "employe_id")
  private UUID employeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type_mouvement", nullable = false, length = 20)
  private TypeMouvement typeMouvement;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private EtatMouvement etat;

  @Column(name = "montant_imputation")
  @Builder.Default
  private BigDecimal montantImputation = BigDecimal.ZERO;

  @Column(columnDefinition = "TEXT")
  private String commentaire;

  @Column(name = "effectue_par_id")
  private Long effectueParId;
}
