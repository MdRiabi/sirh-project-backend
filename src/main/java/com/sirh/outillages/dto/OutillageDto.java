package com.sirh.outillages.dto;

import com.sirh.outillages.entity.Outillage;
import com.sirh.outillages.entity.OutillageEtat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OutillageDto(
        Long id,
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

    public static OutillageDto from(Outillage outillage, String employeNom, String employePrenom) {
        return new OutillageDto(outillage.getId(), outillage.getCodeBarres(), outillage.getNomOutil(),
                outillage.getEtat(), outillage.getAttribueAEmployeId(), employeNom, employePrenom,
                outillage.getDateAttribution(), outillage.getDepartementId(), outillage.getValeurAchat(),
                outillage.getDateAchat());
    }
}
