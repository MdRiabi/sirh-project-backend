package com.sirh.parametres.repository;

import com.sirh.parametres.entity.ParametreGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametreGlobalRepository extends JpaRepository<ParametreGlobal, Long> {

    Optional<ParametreGlobal> findByCle(String cle);
}
