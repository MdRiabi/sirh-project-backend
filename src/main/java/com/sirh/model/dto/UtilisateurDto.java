package com.sirh.model.dto;

import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

public record UtilisateurDto(
        UUID reference,
        UUID employeId,
        String employeNom,
        String employePrenom,
        String username,
        Role role,
        Boolean compteActif,
        LocalDateTime dernierLogin,
        LocalDateTime verrouilleJusquA,
        LocalDateTime createdAt
) {
    public static UtilisateurDto from(Utilisateur utilisateur) {
        var employe = utilisateur.getEmploye();
        return new UtilisateurDto(
                utilisateur.getReference(),
                employe.getId(),
                employe.getNom(),
                employe.getPrenom(),
                utilisateur.getUsername(),
                utilisateur.getRole(),
                utilisateur.getCompteActif(),
                utilisateur.getDernierLogin(),
                utilisateur.getVerrouilleJusquA(),
                utilisateur.getCreatedAt()
        );
    }
}