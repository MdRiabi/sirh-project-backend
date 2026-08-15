package com.sirh.model.repository;

import com.sirh.model.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartementRepository extends JpaRepository<Departement, Long> {

    Optional<Departement> findByReferenceAndDeletedAtIsNull(UUID reference);
}
