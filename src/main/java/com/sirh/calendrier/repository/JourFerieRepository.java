package com.sirh.calendrier.repository;

import com.sirh.calendrier.entity.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {

    List<JourFerie> findByDateBetweenOrderByDate(LocalDate debut, LocalDate fin);
}
