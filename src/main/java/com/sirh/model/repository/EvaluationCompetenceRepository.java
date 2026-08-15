package com.sirh.model.repository;

import com.sirh.model.entity.EvaluationCompetence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCompetenceRepository extends JpaRepository<EvaluationCompetence, Long> {

    List<EvaluationCompetence> findByEvaluationIdAndDeletedAtIsNull(Long evaluationId);

    List<EvaluationCompetence> findByEvaluationIdInAndDeletedAtIsNull(List<Long> evaluationIds);

    void deleteByEvaluationId(Long evaluationId);
}
