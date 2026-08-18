package com.sirh.model.repository;

import com.sirh.model.entity.ParametreGlobal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametreGlobalRepository extends JpaRepository<ParametreGlobal, Long> {

  Optional<ParametreGlobal> findByReferenceAndDeletedAtIsNull(UUID reference);

  Optional<ParametreGlobal> findByCleAndDeletedAtIsNull(String cle);
}
