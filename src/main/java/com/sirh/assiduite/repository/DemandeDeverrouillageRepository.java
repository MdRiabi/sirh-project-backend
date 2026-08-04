package com.sirh.assiduite.repository;

import com.sirh.assiduite.entity.DemandeDeverrouillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeDeverrouillageRepository extends JpaRepository<DemandeDeverrouillage, Long> {

    List<DemandeDeverrouillage> findByStatut(String statut);
}
