package com.sirh.model.repository;

import com.sirh.model.entity.PlanDeveloppement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanDeveloppementRepository extends JpaRepository<PlanDeveloppement, Long> {

  Optional<PlanDeveloppement> findByReferenceAndDeletedAtIsNull(UUID reference);

  List<PlanDeveloppement> findByEvaluationIdAndDeletedAtIsNull(Long evaluationId);

  List<PlanDeveloppement> findByEvaluationIdInAndDeletedAtIsNull(List<Long> evaluationIds);
}
