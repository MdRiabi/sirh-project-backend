package com.sirh.model.repository;

import com.sirh.model.entity.TacheRenouvellement;
import com.sirh.model.enums.TacheRenouvellementStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TacheRenouvellementRepository extends JpaRepository<TacheRenouvellement, Long> {

    Optional<TacheRenouvellement> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<TacheRenouvellement> findByStatutAndDeletedAtIsNullOrderByDateEcheanceAsc(TacheRenouvellementStatut statut);

    List<TacheRenouvellement> findAllByDeletedAtIsNullOrderByDateEcheanceAsc();

    // Bascule quotidienne EN_COURS -> URGENT_EXPIRE (Module 12, cas exceptionnel).
    List<TacheRenouvellement> findByStatutAndDeletedAtIsNull(TacheRenouvellementStatut statut);

    boolean existsByContratIdAndTypeEcheanceAndDeletedAtIsNull(Long contratId, String typeEcheance);
}
