package com.sirh.model.repository;

import com.sirh.model.entity.Outillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutillageRepository extends JpaRepository<Outillage, Long> {

    Optional<Outillage> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<Outillage> findByDepartementIdAndDeletedAtIsNull(Long departementId);
}
