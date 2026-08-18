package com.sirh.model.repository;

import com.sirh.model.entity.EvenementHistorique;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EvenementHistoriqueRepository
    extends JpaRepository<EvenementHistorique, Long>,
        JpaSpecificationExecutor<EvenementHistorique> {

  Optional<EvenementHistorique> findByReferenceAndDeletedAtIsNull(UUID reference);
}
