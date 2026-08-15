package com.sirh.model.repository;

import com.sirh.model.entity.EvenementHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EvenementHistoriqueRepository extends JpaRepository<EvenementHistorique, Long>, JpaSpecificationExecutor<EvenementHistorique> {
}