package com.sirh.personnel.repository;

import com.sirh.personnel.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeIdOrderByDateDebutDesc(UUID employeId);

    Optional<Contrat> findFirstByEmployeIdOrderByDateDebutDesc(UUID employeId);
}