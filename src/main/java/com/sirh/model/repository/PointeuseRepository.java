package com.sirh.model.repository;

import com.sirh.model.entity.Pointeuse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointeuseRepository extends JpaRepository<Pointeuse, Long> {
  Optional<Pointeuse> findByReferenceAndDeletedAtIsNull(UUID reference);

  Optional<Pointeuse> findByAdresseIpAndDeletedAtIsNull(String adresseIp);
}
