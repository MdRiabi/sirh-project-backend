package com.sirh.model.repository;

import com.sirh.model.entity.Evaluation;
import com.sirh.model.enums.EvaluationStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByEmployeIdAndDeletedAtIsNullOrderByAnneeDesc(UUID employeId);

    List<Evaluation> findByStatutAndDeletedAtIsNullOrderByAnneeDescCreatedAtDesc(EvaluationStatut statut);

    List<Evaluation> findByStatutAndAnneeAndDeletedAtIsNullOrderByCreatedAtDesc(EvaluationStatut statut, Integer annee);

    List<Evaluation> findAllByDeletedAtIsNullOrderByAnneeDescCreatedAtDesc();

    boolean existsByEmployeIdAndAnneeAndDeletedAtIsNull(UUID employeId, Integer annee);
}
