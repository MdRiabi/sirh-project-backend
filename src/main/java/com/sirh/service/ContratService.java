package com.sirh.service;

import com.sirh.model.dto.ContratDto;
import com.sirh.model.dto.ContratRequest;
import com.sirh.model.entity.Contrat;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.EvenementHistorique;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.EvenementCategorie;
import com.sirh.model.enums.StatutBadge;
import com.sirh.model.repository.ContratRepository;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.EvenementHistoriqueRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * employes.type_contrat/date_fin_contrat sont dérivés du contrat le plus récent
 * (cahier-technique.md §3.3) : chaque création réévalue ces deux champs plutôt que de supposer que
 * l'insertion la plus récente est chronologiquement la dernière.
 */
@Service
@RequiredArgsConstructor
public class ContratService {

  private final ContratRepository contratRepository;
  private final EvenementHistoriqueRepository evenementRepository;
  private final EmployeService employeService;
  private final EmployeRepository employeRepository;
  private final ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<ContratDto> findByEmploye(UUID employeId) {
    employeService.findById(employeId);
    return contratRepository
        .findByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(employeId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public ContratDto createContrat(UUID employeId, ContratRequest request, Long saisiParId) {
    Employe employe = employeService.findById(employeId);

    Contrat contrat =
        Contrat.builder()
            .employeId(employeId)
            .type(request.type())
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .statut(request.statut() != null ? request.statut() : ContratStatut.EN_COURS)
            .motifFin(request.motifFin())
            .build();
    contrat = contratRepository.save(contrat);

    syncEmployeFromLatestContrat(employe, employeId);
    recordTimelineEntry(employeId, contrat, saisiParId);

    return toDto(contrat);
  }

  private void syncEmployeFromLatestContrat(Employe employe, UUID employeId) {
    Contrat latest =
        contratRepository
            .findFirstByEmployeIdAndDeletedAtIsNullOrderByDateDebutDesc(employeId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Contrat introuvable juste après création pour l'employé " + employeId));
    employe.setTypeContrat(latest.getType().name());
    employe.setDateFinContrat(latest.getDateFin());
    employeRepository.save(employe);
  }

  private void recordTimelineEntry(UUID employeId, Contrat contrat, Long saisiParId) {
    EvenementHistorique evenement =
        EvenementHistorique.builder()
            .employeId(employeId)
            .titre("Nouveau contrat : " + contrat.getType())
            .description(
                contrat.getMotifFin() != null ? "Motif de fin : " + contrat.getMotifFin() : null)
            .categorie(EvenementCategorie.CONTRAT)
            .statutBadge(StatutBadge.PRIMARY)
            .saisiParId(saisiParId)
            .dateEvenement(contrat.getDateDebut().atStartOfDay())
            .build();
    evenementRepository.save(evenement);
  }

  private ContratDto toDto(Contrat contrat) {
    return modelMapper.map(contrat, ContratDto.class);
  }
}
