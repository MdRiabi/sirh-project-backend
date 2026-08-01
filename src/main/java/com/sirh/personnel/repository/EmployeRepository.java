package com.sirh.personnel.repository;

import com.sirh.personnel.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeRepository extends JpaRepository<Employe, UUID> {
}