package com.sirh.model.repository;

import com.sirh.model.entity.PlanDeveloppement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanDeveloppementRepository extends JpaRepository<PlanDeveloppement, Long> {

    List<PlanDeveloppement> findByEvaluationIdAndDeletedAtIsNull(Long evaluationId);

    List<PlanDeveloppement> findByEvaluationIdInAndDeletedAtIsNull(List<Long> evaluationIds);
}
