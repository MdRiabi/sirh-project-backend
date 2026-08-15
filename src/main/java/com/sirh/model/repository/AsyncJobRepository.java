package com.sirh.model.repository;

import com.sirh.model.entity.AsyncJob;
import com.sirh.model.enums.AsyncJobType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {
    List<AsyncJob> findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(AsyncJobType type);
}