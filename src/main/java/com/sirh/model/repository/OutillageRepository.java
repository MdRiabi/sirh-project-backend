package com.sirh.model.repository;

import com.sirh.model.entity.Outillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutillageRepository extends JpaRepository<Outillage, Long> {

    List<Outillage> findByDepartementIdAndDeletedAtIsNull(Long departementId);
}
