package com.sirh.personnel.repository;

import com.sirh.personnel.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeRepository extends JpaRepository<Employe, UUID>, JpaSpecificationExecutor<Employe> {
    // matricule_interne = "badgeId" transmis par les pointeuses Hikvision et par
    // la colonne badgeId des imports Excel (cahier-technique.md §3.2).
    Optional<Employe> findByMatriculeInterne(String matriculeInterne);

    List<Employe> findByDepartement_Id(Long departementId);
}