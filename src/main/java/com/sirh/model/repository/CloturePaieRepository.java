package com.sirh.model.repository;

import com.sirh.model.entity.CloturePaie;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloturePaieRepository extends JpaRepository<CloturePaie, Long> {

  Optional<CloturePaie> findByReferenceAndDeletedAtIsNull(UUID reference);

  Optional<CloturePaie> findByMoisAndDeletedAtIsNull(LocalDate mois);

  // KPI "taux d'anomalies justifiées" (Lot 11, Dashboard) : dernière clôture
  // connue, tous départements confondus (métrique globale de paie).
  Optional<CloturePaie> findTopByDeletedAtIsNullOrderByMoisDesc();
}
