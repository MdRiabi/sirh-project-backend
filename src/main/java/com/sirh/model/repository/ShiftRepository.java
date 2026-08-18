package com.sirh.model.repository;

import com.sirh.model.entity.Shift;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

  Optional<Shift> findByReferenceAndDeletedAtIsNull(UUID reference);

  // Le shift "système" (seedé en V5) sert de support générique aux créneaux
  // CONGE/ABSENCE générés automatiquement (com.sirh.demandes, Lot 5) : ces
  // entrées n'ont pas de créneau horaire réel, mais shift_id reste NOT NULL.
  Optional<Shift> findFirstByTypeRotationAndDeletedAtIsNull(String typeRotation);
}
