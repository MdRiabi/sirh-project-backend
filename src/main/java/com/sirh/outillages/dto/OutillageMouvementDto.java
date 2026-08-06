package com.sirh.outillages.dto;

import com.sirh.outillages.entity.EtatMouvement;
import com.sirh.outillages.entity.OutillageMouvement;
import com.sirh.outillages.entity.TypeMouvement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OutillageMouvementDto(
        Long id,
        Long outillageId,
        UUID employeId,
        TypeMouvement typeMouvement,
        EtatMouvement etat,
        BigDecimal montantImputation,
        String commentaire,
        Long effectueParId,
        LocalDateTime dateMouvement) {

    public static OutillageMouvementDto from(OutillageMouvement mouvement) {
        return new OutillageMouvementDto(mouvement.getId(), mouvement.getOutillageId(), mouvement.getEmployeId(),
                mouvement.getTypeMouvement(), mouvement.getEtat(), mouvement.getMontantImputation(),
                mouvement.getCommentaire(), mouvement.getEffectueParId(), mouvement.getDateMouvement());
    }
}
