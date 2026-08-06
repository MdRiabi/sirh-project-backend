package com.sirh.outillages.repository;

import com.sirh.outillages.entity.OutillageMouvement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutillageMouvementRepository extends JpaRepository<OutillageMouvement, Long> {

    List<OutillageMouvement> findByOutillageIdOrderByDateMouvementDesc(Long outillageId);
}
