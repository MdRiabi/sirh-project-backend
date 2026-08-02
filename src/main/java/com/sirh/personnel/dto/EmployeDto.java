package com.sirh.personnel.dto;

import com.sirh.personnel.entity.Employe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeDto(
        UUID id,
        String matriculeInterne,
        String matriculeCnss,
        String matriculePaie,
        String nom,
        String prenom,
        String email,
        Long departementId,
        String departementNom,
        String typeContrat,
        LocalDate dateEmbauche,
        LocalDate dateFinContrat,
        BigDecimal soldeCongesJours,
        Boolean actif,
        LocalDateTime createdAt
) {
    public static EmployeDto from(Employe employe) {
        return new EmployeDto(
                employe.getId(),
                employe.getMatriculeInterne(),
                maskCnss(employe.getMatriculeCnss()),
                employe.getMatriculePaie(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getDepartementId(),
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                employe.getTypeContrat(),
                employe.getDateEmbauche(),
                employe.getDateFinContrat(),
                employe.getSoldeCongesJours(),
                employe.getActif(),
                employe.getCreatedAt()
        );
    }

    // Masquage partiel par défaut (ARCHITECTURE.md §10) : "***-**-1234".
    // Affichage complet réservé à GET /employes/{id}/matricule-cnss.
    private static String maskCnss(String raw) {
        if (raw == null) {
            return null;
        }
        String last4 = raw.length() <= 4 ? raw : raw.substring(raw.length() - 4);
        return "***-**-" + last4;
    }
}