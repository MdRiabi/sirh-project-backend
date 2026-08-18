package com.sirh.model.repository;

import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.EntiteType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichierRepository extends JpaRepository<Fichier, Long> {
  Optional<Fichier> findByReferenceAndDeletedAtIsNull(UUID reference);

  List<Fichier> findByEntiteTypeAndEntiteIdAndDeletedAtIsNull(EntiteType entiteType, Long entiteId);
}
