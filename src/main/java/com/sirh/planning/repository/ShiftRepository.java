package com.sirh.planning.repository;

import com.sirh.planning.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    // Le shift "système" (seedé en V5) sert de support générique aux créneaux
    // CONGE/ABSENCE générés automatiquement (com.sirh.demandes, Lot 5) : ces
    // entrées n'ont pas de créneau horaire réel, mais shift_id reste NOT NULL.
    Optional<Shift> findFirstByTypeRotation(String typeRotation);
}