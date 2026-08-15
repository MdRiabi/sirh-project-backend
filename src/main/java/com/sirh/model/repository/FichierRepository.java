package com.sirh.model.repository;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.entity.Fichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichierRepository extends JpaRepository<Fichier, Long> {
    List<Fichier> findByEntiteTypeAndEntiteIdAndDeletedAtIsNull(EntiteType entiteType, Long entiteId);
}
