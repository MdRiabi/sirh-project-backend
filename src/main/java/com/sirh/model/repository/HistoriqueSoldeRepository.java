package com.sirh.model.repository;

import com.sirh.model.entity.HistoriqueSolde;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HistoriqueSoldeRepository extends JpaRepository<HistoriqueSolde, Long> {

    Optional<HistoriqueSolde> findByReferenceAndDeletedAtIsNull(UUID reference);
}
