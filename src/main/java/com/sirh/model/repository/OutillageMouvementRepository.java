package com.sirh.model.repository;

import com.sirh.model.entity.OutillageMouvement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutillageMouvementRepository extends JpaRepository<OutillageMouvement, Long> {

    List<OutillageMouvement> findByOutillageIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long outillageId);
}
