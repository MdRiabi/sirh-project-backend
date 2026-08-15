package com.sirh.model.repository;

import com.sirh.model.entity.ParametreGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametreGlobalRepository extends JpaRepository<ParametreGlobal, Long> {

    Optional<ParametreGlobal> findByCleAndDeletedAtIsNull(String cle);
}
