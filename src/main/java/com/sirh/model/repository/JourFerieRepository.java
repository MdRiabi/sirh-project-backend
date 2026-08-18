package com.sirh.model.repository;

import com.sirh.model.entity.JourFerie;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {

  Optional<JourFerie> findByReferenceAndDeletedAtIsNull(UUID reference);

  List<JourFerie> findByDateBetweenAndDeletedAtIsNullOrderByDate(LocalDate debut, LocalDate fin);
}
