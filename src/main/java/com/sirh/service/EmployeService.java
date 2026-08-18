package com.sirh.service;

import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.EmployeDto;
import com.sirh.model.dto.EmployeRequest;
import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.DepartementRepository;
import com.sirh.model.repository.EmployeRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeService {

  private final EmployeRepository employeRepository;
  private final DepartementRepository departementRepository;
  private final ModelMapper modelMapper;

  // Specification plutôt qu'un @Query "(:param IS NULL OR ...)" : ce dernier a
  // cassé en pratique (pgjdbc ne sait pas toujours déduire le type d'un
  // paramètre nul dès qu'il apparaît sans contexte de type, notamment une fois
  // que Postgres bascule sur un plan préparé mis en cache après quelques
  // appels — "function lower(bytea) does not exist"). Une Specification omet
  // purement et simplement le prédicat quand le filtre est absent : aucun
  // paramètre nul ambigu n'est jamais envoyé à Postgres.
  @Transactional(readOnly = true)
  public Page<EmployeDto> search(
      String search, Long departementId, Boolean actif, Pageable pageable) {
    Specification<Employe> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("nom")), pattern),
                    cb.like(cb.lower(root.get("prenom")), pattern),
                    cb.like(cb.lower(root.get("matriculeInterne")), pattern)));
          }
          if (departementId != null) {
            predicates.add(cb.equal(root.get("departement").get("id"), departementId));
          }
          if (actif != null) {
            predicates.add(cb.equal(root.get("actif"), actif));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return employeRepository.findAll(spec, pageable).map(this::toDto);
  }

  // Retourne l'entité (pas le DTO) : réutilisé en interne par ContratService,
  // EvenementHistoriqueService, et par revealMatriculeCnss qui a besoin du
  // matricule_cnss en clair, jamais exposé via EmployeDto.
  @Transactional(readOnly = true)
  public Employe findById(UUID id) {
    return employeRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException(ErrorCode.EMPLOYE_NOT_FOUND, new Object[] {id}));
  }

  @Transactional(readOnly = true)
  public EmployeDto getEmployeDto(UUID id) {
    return toDto(findById(id));
  }

  // Affichage complet du matricule_cnss réservé à une action explicite RH/SuperUser
  // (ARCHITECTURE.md §10) — jamais renvoyé automatiquement dans la liste/fiche.
  @Transactional(readOnly = true)
  public String revealMatriculeCnss(UUID id) {
    Employe employe = findById(id);
    return employe.getMatriculeCnss() != null ? employe.getMatriculeCnss() : "";
  }

  @Transactional
  public EmployeDto create(EmployeRequest request) {
    Employe employe =
        Employe.builder()
            .matriculeInterne(request.matriculeInterne())
            .matriculeCnss(request.matriculeCnss())
            .matriculePaie(request.matriculePaie())
            .nom(request.nom())
            .prenom(request.prenom())
            .email(request.email())
            .departement(resolveDepartement(request.departementId()))
            .typeContrat(request.typeContrat())
            .dateEmbauche(request.dateEmbauche())
            .dateFinContrat(request.dateFinContrat())
            .actif(request.actif() != null ? request.actif() : Boolean.TRUE)
            .build();
    return toDto(employeRepository.save(employe));
  }

  @Transactional
  public EmployeDto update(UUID id, EmployeRequest request) {
    Employe employe = findById(id);
    employe.setMatriculeInterne(request.matriculeInterne());
    // matriculeCnss n'est jamais renvoyé en clair par l'API (masquage
    // ARCHITECTURE.md §10) : le formulaire d'édition ne peut donc jamais le
    // ré-émettre tel quel. Un champ vide signifie "ne pas modifier", pas
    // "effacer" — sinon toute édition sans ressaisie du CNSS l'efface.
    if (request.matriculeCnss() != null && !request.matriculeCnss().isBlank()) {
      employe.setMatriculeCnss(request.matriculeCnss());
    }
    employe.setMatriculePaie(request.matriculePaie());
    employe.setNom(request.nom());
    employe.setPrenom(request.prenom());
    employe.setEmail(request.email());
    employe.setDepartement(resolveDepartement(request.departementId()));
    employe.setTypeContrat(request.typeContrat());
    employe.setDateEmbauche(request.dateEmbauche());
    employe.setDateFinContrat(request.dateFinContrat());
    if (request.actif() != null) {
      employe.setActif(request.actif());
    }
    return toDto(employeRepository.save(employe));
  }

  private Departement resolveDepartement(Long departementId) {
    if (departementId == null) {
      return null;
    }
    return departementRepository
        .findById(departementId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.DEPARTEMENT_NOT_FOUND, new Object[] {departementId}));
  }

  private EmployeDto toDto(Employe employe) {
    EmployeDto dto = modelMapper.map(employe, EmployeDto.class);
    dto.setMatriculeCnss(maskCnss(employe.getMatriculeCnss()));
    dto.setDepartementNom(
        employe.getDepartement() != null ? employe.getDepartement().getNom() : null);
    return dto;
  }

  // Masquage partiel par défaut (ARCHITECTURE.md §10) : "***-**-1234".
  // Affichage complet réservé à GET /employes/{id}/matricule-cnss.
  private static String maskCnss(String raw) {
    if (raw == null) {
      return null;
    }
    String last4 = raw.length() <= 4 ? raw : raw.substring(raw.length() - 4);
    return "***-**-" + last4;
  }
}
