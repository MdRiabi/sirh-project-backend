package com.sirh.model.repository;

import com.sirh.model.entity.Contrat;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(UUID employeId);

    Optional<Contrat> findFirstByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(UUID employeId);

    // Alerte mensuelle sous-activité (Module 11, com.sirh.notifications) :
    // "pour un contrat actif" — candidats du calcul de jours travaillés.
    List<Contrat> findByStatutAndDeletedAtIsNull(ContratStatut statut);

    // Renouvellement de contrats (Module 12, com.sirh.renouvellement) : seuls
    // les CDD sont concernés par M+5/M+11 (un CDI n'a pas de terme à anticiper).
    List<Contrat> findByStatutAndTypeAndDeletedAtIsNull(ContratStatut statut, ContratType type);
}