package com.sirh.model.repository;

import com.sirh.model.entity.EvenementHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface EvenementHistoriqueRepository extends JpaRepository<EvenementHistorique, Long>, JpaSpecificationExecutor<EvenementHistorique> {

    Optional<EvenementHistorique> findByReferenceAndDeletedAtIsNull(UUID reference);
}