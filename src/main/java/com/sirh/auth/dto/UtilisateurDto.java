package com.sirh.auth.dto;

import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

public record UtilisateurDto(
        Long id,
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
                utilisateur.getId(),
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