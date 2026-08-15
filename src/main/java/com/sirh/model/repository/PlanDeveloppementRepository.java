package com.sirh.model.repository;

import com.sirh.model.entity.PlanDeveloppement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanDeveloppementRepository extends JpaRepository<PlanDeveloppement, Long> {

    Optional<PlanDeveloppement> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<PlanDeveloppement> findByEvaluationIdAndDeletedAtIsNull(Long evaluationId);

    List<PlanDeveloppement> findByEvaluationIdInAndDeletedAtIsNull(List<Long> evaluationIds);
}
