package com.sirh.model.repository;

import com.sirh.model.entity.Pointeuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointeuseRepository extends JpaRepository<Pointeuse, Long> {
    Optional<Pointeuse> findByAdresseIpAndDeletedAtIsNull(String adresseIp);
}