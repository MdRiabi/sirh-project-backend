package com.sirh.demandes.repository;

import com.sirh.demandes.entity.Indisponibilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IndisponibiliteRepository extends JpaRepository<Indisponibilite, Long>, JpaSpecificationExecutor<Indisponibilite> {

    // Chevauchement avec un congé VALIDE (cahier-fonctionnel.md Module 2) :
    // indisponibilité.dateDebut <= fin ET indisponibilité.dateFin >= début.
    List<Indisponibilite> findByEmployeIdAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            UUID employeId, String statut, LocalDate dateFin, LocalDate dateDebut);

    // Tous employés confondus : congés PAYÉS validés couvrant un jour férié donné
    // (cahier-fonctionnel.md Module 6 / cahier-technique.md §4.5 : le recrédit ne
    // s'applique qu'au congé payé, pas aux autres types d'indisponibilité).
    List<Indisponibilite> findByTypeIndisponibiliteAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            String typeIndisponibilite, String statut, LocalDate date, LocalDate sameDate);

    // Contrôle préalable à la clôture de paie (Module 3, com.sirh.assiduite) :
    // tous employés confondus, même principe de chevauchement que le contrôle
    // planning (cahier-technique.md §3.9/§4.4).
    List<Indisponibilite> findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            String statut, LocalDate finMois, LocalDate debutMois);
}