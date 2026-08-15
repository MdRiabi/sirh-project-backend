package com.sirh.model.repository;

import com.sirh.model.entity.DemandeDeverrouillage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DemandeDeverrouillageRepository
        extends JpaRepository<DemandeDeverrouillage, Long>, JpaSpecificationExecutor<DemandeDeverrouillage> {
}
