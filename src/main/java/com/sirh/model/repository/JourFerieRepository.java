package com.sirh.model.repository;

import com.sirh.model.entity.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {

    List<JourFerie> findByDateBetweenAndDeletedAtIsNullOrderByDate(LocalDate debut, LocalDate fin);
}
