package com.sirh.audit.repository;

import com.sirh.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Lecture réservée SUPERUSER (cf. AuditLog, ARCHITECTURE.md §9) — la
// pagination vient gratuitement de JpaRepository/PagingAndSortingRepository,
// JpaSpecificationExecutor porte les filtres optionnels (action/tableCible/
// utilisateurId/plage de dates), même pattern que demandes/pointages.
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
