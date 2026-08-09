package com.sirh.audit.service;

import com.sirh.audit.dto.AuditLogDto;
import com.sirh.audit.entity.AuditLog;
import com.sirh.audit.repository.AuditLogRepository;
import com.sirh.audit.repository.AuditLogSpecifications;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UtilisateurRepository utilisateurRepository) {
        this.auditLogRepository = auditLogRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public Page<AuditLogDto> rechercher(String action, String tableCible, Long utilisateurId,
                                          LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.avecFiltres(action, tableCible, utilisateurId, dateDebut, dateFin), pageable);

        var utilisateurIds = page.getContent().stream()
                .map(AuditLog::getUtilisateurId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> nomsParUtilisateur = utilisateurRepository.findAllById(utilisateurIds).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Utilisateur::getUsername));

        return page.map(log -> new AuditLogDto(
                log.getId(),
                log.getUtilisateurId(),
                log.getUtilisateurId() != null ? nomsParUtilisateur.get(log.getUtilisateurId()) : null,
                log.getAction(),
                log.getTableCible(),
                log.getAnciennesValeurs(),
                log.getNouvellesValeurs(),
                log.getAdresseIp(),
                log.getCreatedAt()));
    }
}
