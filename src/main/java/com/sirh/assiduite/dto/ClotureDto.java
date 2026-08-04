package com.sirh.assiduite.dto;

import com.sirh.assiduite.entity.CloturePaie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClotureDto(
        Long id,
        LocalDate mois,
        String statut,
        LocalDateTime dateCloture,
        UUID clotureParId,
        BigDecimal tauxAnomaliesJustifiees) {

    public static ClotureDto from(CloturePaie cloture) {
        return new ClotureDto(cloture.getId(), cloture.getMois(), cloture.getStatut(), cloture.getDateCloture(),
                cloture.getClotureParId(), cloture.getTauxAnomaliesJustifiees());
    }
}
