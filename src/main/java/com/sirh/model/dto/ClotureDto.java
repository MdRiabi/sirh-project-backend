package com.sirh.model.dto;

import com.sirh.model.entity.CloturePaie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClotureDto(
        UUID reference,
        LocalDate mois,
        String statut,
        LocalDateTime dateCloture,
        UUID clotureParId,
        BigDecimal tauxAnomaliesJustifiees) {

    public static ClotureDto from(CloturePaie cloture) {
        return new ClotureDto(cloture.getReference(), cloture.getMois(), cloture.getStatut(), cloture.getDateCloture(),
                cloture.getClotureParId(), cloture.getTauxAnomaliesJustifiees());
    }
}
