package com.sirh.model.repository;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.entity.Fichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FichierRepository extends JpaRepository<Fichier, Long> {
    Optional<Fichier> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<Fichier> findByEntiteTypeAndEntiteIdAndDeletedAtIsNull(EntiteType entiteType, Long entiteId);
}
