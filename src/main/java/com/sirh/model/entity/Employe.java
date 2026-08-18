package com.sirh.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "employes")
@SQLDelete(sql = "UPDATE employes SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "matricule_interne", nullable = false)
  private String matriculeInterne;

  @Column(name = "matricule_cnss")
  private String matriculeCnss;

  @Column(name = "matricule_paie")
  private String matriculePaie;

  @Column(nullable = false)
  private String nom;

  @Column(nullable = false)
  private String prenom;

  @Column() private String email;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departement_id")
  private Departement departement;

  // Conservé pour ne pas propager la relation JPA à tous les appelants existants
  // (JWT/SirhUserDetails/AuthService, cf. Lot 0) qui ne lisent qu'un identifiant.
  public Long getDepartementId() {
    return departement != null ? departement.getId() : null;
  }

  @Column(name = "type_contrat", nullable = false)
  private String typeContrat;

  @Column(name = "date_embauche", nullable = false)
  private LocalDate dateEmbauche;

  @Column(name = "date_fin_contrat")
  private LocalDate dateFinContrat;

  @Column(name = "solde_conges_jours")
  private BigDecimal soldeCongesJours;

  private Boolean actif;
}
