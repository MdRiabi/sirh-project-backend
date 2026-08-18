package com.sirh.service;

import com.sirh.model.dto.AuditLogDto;
import com.sirh.model.entity.AuditLog;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.repository.AuditLogRepository;
import com.sirh.model.repository.AuditLogSpecifications;
import com.sirh.model.repository.UtilisateurRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final UtilisateurRepository utilisateurRepository;
  private final ModelMapper modelMapper;

  public Page<AuditLogDto> rechercher(
      String action,
      String tableCible,
      Long utilisateurId,
      LocalDateTime dateDebut,
      LocalDateTime dateFin,
      Pageable pageable) {
    Page<AuditLog> page =
        auditLogRepository.findAll(
            AuditLogSpecifications.avecFiltres(
                action, tableCible, utilisateurId, dateDebut, dateFin),
            pageable);

    var utilisateurIds =
        page.getContent().stream()
            .map(AuditLog::getUtilisateurId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, String> nomsParUtilisateur =
        utilisateurRepository.findAllById(utilisateurIds).stream()
            .collect(Collectors.toMap(Utilisateur::getId, Utilisateur::getUsername));

    return page.map(
        log -> {
          AuditLogDto dto = modelMapper.map(log, AuditLogDto.class);
          dto.setUtilisateurNom(
              log.getUtilisateurId() != null
                  ? nomsParUtilisateur.get(log.getUtilisateurId())
                  : null);
          return dto;
        });
  }
}
