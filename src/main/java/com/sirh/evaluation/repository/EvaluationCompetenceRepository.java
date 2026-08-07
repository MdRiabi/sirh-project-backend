package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.EvaluationCompetence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCompetenceRepository extends JpaRepository<EvaluationCompetence, Long> {

    List<EvaluationCompetence> findByEvaluationId(Long evaluationId);

    List<EvaluationCompetence> findByEvaluationIdIn(List<Long> evaluationIds);

    void deleteByEvaluationId(Long evaluationId);
}
