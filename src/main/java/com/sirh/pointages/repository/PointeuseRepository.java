package com.sirh.pointages.repository;

import com.sirh.pointages.entity.Pointeuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointeuseRepository extends JpaRepository<Pointeuse, Long> {
    Optional<Pointeuse> findByAdresseIp(String adresseIp);
}