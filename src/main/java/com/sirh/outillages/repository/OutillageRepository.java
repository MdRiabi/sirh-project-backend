package com.sirh.outillages.repository;

import com.sirh.outillages.entity.Outillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutillageRepository extends JpaRepository<Outillage, Long> {

    List<Outillage> findByDepartementId(Long departementId);
}
