package com.sirh.model.dto;

import com.sirh.model.enums.OutillageEtat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OutillageDto(
        UUID reference,
        String codeBarres,
        String nomOutil,
        OutillageEtat etat,
        UUID attribueAEmployeId,
        String attribueAEmployeNom,
        String attribueAEmployePrenom,
        LocalDateTime dateAttribution,
        Long departementId,
        BigDecimal valeurAchat,
        LocalDate dateAchat) {
}
