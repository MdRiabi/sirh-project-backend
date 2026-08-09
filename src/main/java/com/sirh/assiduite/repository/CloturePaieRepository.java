package com.sirh.assiduite.repository;

import com.sirh.assiduite.entity.CloturePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CloturePaieRepository extends JpaRepository<CloturePaie, Long> {

    Optional<CloturePaie> findByMois(LocalDate mois);

    // KPI "taux d'anomalies justifiées" (Lot 11, Dashboard) : dernière clôture
    // connue, tous départements confondus (métrique globale de paie).
    Optional<CloturePaie> findTopByOrderByMoisDesc();
}
