package com.sirh.model.repository;

import com.sirh.model.entity.AsyncJob;
import com.sirh.model.enums.AsyncJobType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {
    Optional<AsyncJob> findByReferenceAndDeletedAtIsNull(UUID reference);

    List<AsyncJob> findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(AsyncJobType type);
}