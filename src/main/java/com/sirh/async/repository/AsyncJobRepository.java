package com.sirh.async.repository;

import com.sirh.async.entity.AsyncJob;
import com.sirh.async.entity.AsyncJobType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {
    List<AsyncJob> findByTypeOrderByCreatedAtDesc(AsyncJobType type);
}