package com.sirh.fichiers.repository;

import com.sirh.fichiers.entity.EntiteType;
import com.sirh.fichiers.entity.Fichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichierRepository extends JpaRepository<Fichier, Long> {
    List<Fichier> findByEntiteTypeAndEntiteId(EntiteType entiteType, Long entiteId);
}
