package com.sirh.model.repository;

import com.sirh.model.entity.OutillageMouvement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutillageMouvementRepository extends JpaRepository<OutillageMouvement, Long> {

  Optional<OutillageMouvement> findByReferenceAndDeletedAtIsNull(UUID reference);

  List<OutillageMouvement> findByOutillageIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      Long outillageId);
}
