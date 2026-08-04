package com.sirh.demandes.repository;

import com.sirh.demandes.entity.Autorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AutorisationRepository extends JpaRepository<Autorisation, Long>, JpaSpecificationExecutor<Autorisation> {

    // Cumul mensuel pour le contrôle de quota (Module 13, 4h/mois) : statuts
    // EN_ATTENTE_RH + VALIDE inclus (une demande en attente réserve déjà le
    // quota, pour alerter au plus tôt plutôt qu'au moment de la validation).
    List<Autorisation> findByEmployeIdAndDateAutorisationBetweenAndStatutIn(
            UUID employeId, LocalDate debut, LocalDate fin, List<String> statuts);

    // Contrôle préalable à la clôture de paie (Module 3, com.sirh.assiduite) :
    // aucune demande ne doit rester EN_ATTENTE_RH sur le mois à clôturer.
    List<Autorisation> findByStatutAndDateAutorisationBetween(String statut, LocalDate debut, LocalDate fin);
}
