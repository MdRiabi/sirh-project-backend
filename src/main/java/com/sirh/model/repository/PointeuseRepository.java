package com.sirh.model.repository;

import com.sirh.model.entity.Pointeuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PointeuseRepository extends JpaRepository<Pointeuse, Long> {
    Optional<Pointeuse> findByReferenceAndDeletedAtIsNull(UUID reference);

    Optional<Pointeuse> findByAdresseIpAndDeletedAtIsNull(String adresseIp);
}