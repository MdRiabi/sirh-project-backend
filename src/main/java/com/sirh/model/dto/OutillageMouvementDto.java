package com.sirh.model.dto;

import com.sirh.model.enums.EtatMouvement;
import com.sirh.model.entity.OutillageMouvement;
import com.sirh.model.enums.TypeMouvement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OutillageMouvementDto(
        UUID reference,
        Long outillageId,
        UUID employeId,
        TypeMouvement typeMouvement,
        EtatMouvement etat,
        BigDecimal montantImputation,
        String commentaire,
        Long effectueParId,
        LocalDateTime dateMouvement) {

    public static OutillageMouvementDto from(OutillageMouvement mouvement) {
        return new OutillageMouvementDto(mouvement.getReference(), mouvement.getOutillageId(), mouvement.getEmployeId(),
                mouvement.getTypeMouvement(), mouvement.getEtat(), mouvement.getMontantImputation(),
                mouvement.getCommentaire(), mouvement.getEffectueParId(), mouvement.getCreatedAt());
    }
}
