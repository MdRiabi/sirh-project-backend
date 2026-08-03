package com.sirh.demandes.repository;

import com.sirh.demandes.entity.Indisponibilite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IndisponibiliteRepository extends JpaRepository<Indisponibilite, Long> {

    // Chevauchement avec un congé VALIDE (cahier-fonctionnel.md Module 2) :
    // indisponibilité.dateDebut <= fin ET indisponibilité.dateFin >= début.
    List<Indisponibilite> findByEmployeIdAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            UUID employeId, String statut, LocalDate dateFin, LocalDate dateDebut);
}