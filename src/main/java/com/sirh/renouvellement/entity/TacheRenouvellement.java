package com.sirh.renouvellement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "taches_renouvellement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TacheRenouvellement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private UUID employeId;

    @Column(name = "contrat_id")
    private Long contratId;

    // "M+5" / "M+11" (cahier-technique.md §3.20) : chaîne littérale plutôt
    // qu'un enum Java (le "+" n'est pas un identifiant valide) — contrainte
    // CHECK en base (V9) pour fermer le domaine de valeurs.
    @Column(name = "type_echeance", nullable = false, length = 10)
    private String typeEcheance;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TacheRenouvellementStatut statut = TacheRenouvellementStatut.EN_COURS;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "traite_par_id")
    private Long traiteParId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
