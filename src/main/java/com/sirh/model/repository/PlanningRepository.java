package com.sirh.model.repository;

import com.sirh.model.entity.Planning;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningRepository extends JpaRepository<Planning, Long> {

  Optional<Planning> findByReferenceAndDeletedAtIsNull(UUID reference);

  // Fenêtre élargie autour d'une nouvelle affectation pour contrôler le repos
  // légal de 11h et détecter un chevauchement direct (cahier-technique.md §3.9).
  List<Planning> findByEmployeIdAndDateFinAfterAndDateDebutBeforeAndDeletedAtIsNull(
      UUID employeId, LocalDateTime apres, LocalDateTime avant);

  List<Planning> findByEmployeIdAndDateDebutBetweenAndDeletedAtIsNullOrderByDateDebut(
      UUID employeId, LocalDateTime debut, LocalDateTime fin);

  List<Planning> findByEmployeIdInAndDateDebutBetweenAndDeletedAtIsNullOrderByDateDebut(
      List<UUID> employeIds, LocalDateTime debut, LocalDateTime fin);
}
