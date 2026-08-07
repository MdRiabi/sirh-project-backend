package com.sirh.renouvellement.dto;

import com.sirh.renouvellement.entity.TacheRenouvellement;
import com.sirh.renouvellement.entity.TacheRenouvellementStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TacheRenouvellementDto(
        Long id,
        UUID employeId,
        String employeNom,
        String employePrenom,
        Long contratId,
        String typeEcheance,
        LocalDate dateEcheance,
        TacheRenouvellementStatut statut,
        LocalDateTime dateTraitement,
        Long traiteParId,
        LocalDateTime createdAt
) {
    public static TacheRenouvellementDto from(TacheRenouvellement tache, String employeNom, String employePrenom) {
        return new TacheRenouvellementDto(
                tache.getId(), tache.getEmployeId(), employeNom, employePrenom, tache.getContratId(),
                tache.getTypeEcheance(), tache.getDateEcheance(), tache.getStatut(),
                tache.getDateTraitement(), tache.getTraiteParId(), tache.getCreatedAt());
    }
}
