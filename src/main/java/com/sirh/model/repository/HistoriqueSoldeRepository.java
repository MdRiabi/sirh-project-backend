package com.sirh.model.repository;

import com.sirh.model.entity.HistoriqueSolde;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueSoldeRepository extends JpaRepository<HistoriqueSolde, Long> {

  Optional<HistoriqueSolde> findByReferenceAndDeletedAtIsNull(UUID reference);
}
