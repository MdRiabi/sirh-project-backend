package com.sirh.outillages.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outillage_mouvements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutillageMouvement {

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

    @Generated(event = EventType.INSERT)
    @Column(name = "date_mouvement", insertable = false, updatable = false)
    private LocalDateTime dateMouvement;
}
