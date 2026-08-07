package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.PlanDeveloppement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanDeveloppementRepository extends JpaRepository<PlanDeveloppement, Long> {

    List<PlanDeveloppement> findByEvaluationId(Long evaluationId);

    List<PlanDeveloppement> findByEvaluationIdIn(List<Long> evaluationIds);
}
