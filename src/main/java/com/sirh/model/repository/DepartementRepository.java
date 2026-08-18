package com.sirh.model.repository;

import com.sirh.model.entity.Departement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartementRepository extends JpaRepository<Departement, Long> {

  Optional<Departement> findByReferenceAndDeletedAtIsNull(UUID reference);
}
