package com.sirh.service;

import com.sirh.model.dto.EmployeDto;
import com.sirh.model.dto.EmployeRequest;
import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.exception.DepartementNotFoundException;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.model.repository.DepartementRepository;
import com.sirh.model.repository.EmployeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final DepartementRepository departementRepository;

    public EmployeService(EmployeRepository employeRepository, DepartementRepository departementRepository) {
        this.employeRepository = employeRepository;
        this.departementRepository = departementRepository;
    }

    // Specification plutôt qu'un @Query "(:param IS NULL OR ...)" : ce dernier a
    // cassé en pratique (pgjdbc ne sait pas toujours déduire le type d'un
    // paramètre nul dès qu'il apparaît sans contexte de type, notamment une fois
    // que Postgres bascule sur un plan préparé mis en cache après quelques
    // appels — "function lower(bytea) does not exist"). Une Specification omet
    // purement et simplement le prédicat quand le filtre est absent : aucun
    // paramètre nul ambigu n'est jamais envoyé à Postgres.
    @Transactional(readOnly = true)
    public Page<EmployeDto> search(String search, Long departementId, Boolean actif, Pageable pageable) {
        Specification<Employe> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nom")), pattern),
                        cb.like(cb.lower(root.get("prenom")), pattern),
                        cb.like(cb.lower(root.get("matriculeInterne")), pattern)
                ));
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
        return employeRepository.findById(id).orElseThrow(() -> new EmployeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public EmployeDto getEmployeDto(UUID id) {
        return toDto(findById(id));
    }

    @Transactional
    public EmployeDto create(EmployeRequest request) {
        Employe employe = Employe.builder()
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
        return departementRepository.findById(departementId)
                .orElseThrow(() -> new DepartementNotFoundException(departementId));
    }

    private EmployeDto toDto(Employe employe) {
        return new EmployeDto(
                employe.getId(),
                employe.getMatriculeInterne(),
                maskCnss(employe.getMatriculeCnss()),
                employe.getMatriculePaie(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getDepartementId(),
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                employe.getTypeContrat(),
                employe.getDateEmbauche(),
                employe.getDateFinContrat(),
                employe.getSoldeCongesJours(),
                employe.getActif(),
                employe.getCreatedAt()
        );
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
