package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.Evaluation;
import com.sirh.evaluation.entity.EvaluationStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByEmployeIdOrderByAnneeDesc(UUID employeId);

    List<Evaluation> findByStatutOrderByAnneeDescCreatedAtDesc(EvaluationStatut statut);

    List<Evaluation> findByStatutAndAnneeOrderByCreatedAtDesc(EvaluationStatut statut, Integer annee);

    List<Evaluation> findAllByOrderByAnneeDescCreatedAtDesc();

    boolean existsByEmployeIdAndAnnee(UUID employeId, Integer annee);
}
