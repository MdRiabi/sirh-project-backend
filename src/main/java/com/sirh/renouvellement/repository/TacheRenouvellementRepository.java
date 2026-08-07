package com.sirh.renouvellement.repository;

import com.sirh.renouvellement.entity.TacheRenouvellement;
import com.sirh.renouvellement.entity.TacheRenouvellementStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TacheRenouvellementRepository extends JpaRepository<TacheRenouvellement, Long> {

    List<TacheRenouvellement> findByStatutOrderByDateEcheanceAsc(TacheRenouvellementStatut statut);

    List<TacheRenouvellement> findAllByOrderByDateEcheanceAsc();

    // Bascule quotidienne EN_COURS -> URGENT_EXPIRE (Module 12, cas exceptionnel).
    List<TacheRenouvellement> findByStatut(TacheRenouvellementStatut statut);

    boolean existsByContratIdAndTypeEcheance(Long contratId, String typeEcheance);
}
