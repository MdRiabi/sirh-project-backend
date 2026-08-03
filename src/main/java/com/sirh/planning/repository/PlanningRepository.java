package com.sirh.planning.repository;

import com.sirh.planning.entity.Planning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlanningRepository extends JpaRepository<Planning, Long> {

    // Fenêtre élargie autour d'une nouvelle affectation pour contrôler le repos
    // légal de 11h et détecter un chevauchement direct (cahier-technique.md §3.9).
    List<Planning> findByEmployeIdAndDateFinAfterAndDateDebutBefore(
            UUID employeId, LocalDateTime apres, LocalDateTime avant);

    List<Planning> findByEmployeIdAndDateDebutBetweenOrderByDateDebut(
            UUID employeId, LocalDateTime debut, LocalDateTime fin);

    List<Planning> findByEmployeIdInAndDateDebutBetweenOrderByDateDebut(
            List<UUID> employeIds, LocalDateTime debut, LocalDateTime fin);
}