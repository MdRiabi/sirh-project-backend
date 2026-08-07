package com.sirh.personnel.repository;

import com.sirh.personnel.entity.Contrat;
import com.sirh.personnel.entity.ContratStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeIdOrderByDateDebutDesc(UUID employeId);

    Optional<Contrat> findFirstByEmployeIdOrderByDateDebutDesc(UUID employeId);

    // Alerte mensuelle sous-activité (Module 11, com.sirh.notifications) :
    // "pour un contrat actif" — candidats du calcul de jours travaillés.
    List<Contrat> findByStatut(ContratStatut statut);
}