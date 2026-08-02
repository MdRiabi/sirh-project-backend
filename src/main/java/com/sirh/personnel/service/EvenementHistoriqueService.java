package com.sirh.personnel.service;

import com.sirh.personnel.dto.EvenementRequest;
import com.sirh.personnel.entity.EvenementCategorie;
import com.sirh.personnel.entity.EvenementHistorique;
import com.sirh.personnel.entity.StatutBadge;
import com.sirh.personnel.repository.EvenementHistoriqueRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EvenementHistoriqueService {

    private final EvenementHistoriqueRepository evenementRepository;
    private final EmployeService employeService;

    public EvenementHistoriqueService(EvenementHistoriqueRepository evenementRepository, EmployeService employeService) {
        this.evenementRepository = evenementRepository;
        this.employeService = employeService;
    }

    // Specification plutôt qu'un @Query "(:param IS NULL OR ...)" — même défaut
    // que EmployeService.search : pgjdbc rejette un paramètre temporel nul sans
    // contexte de type dès que Postgres bascule sur un plan préparé mis en
    // cache ("could not determine data type of parameter"). Une Specification
    // n'envoie jamais ce paramètre quand le filtre est absent.
    @Transactional(readOnly = true)
    public List<EvenementHistorique> findTimeline(UUID employeId, EvenementCategorie categorie,
                                                   LocalDateTime dateDebut, LocalDateTime dateFin) {
        employeService.findById(employeId);
        Specification<EvenementHistorique> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("employeId"), employeId));
            if (categorie != null) {
                predicates.add(cb.equal(root.get("categorie"), categorie));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateEvenement"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateEvenement"), dateFin));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return evenementRepository.findAll(spec, Sort.by("dateEvenement").descending());
    }

    @Transactional
    public EvenementHistorique createManualEntry(UUID employeId, EvenementRequest request, Long saisiParId) {
        employeService.findById(employeId);
        // CONTRAT est exclusivement généré par ContratService (règle de synchronisation
        // cahier-technique.md §3.3) : une saisie manuelle ne doit pas créer une entrée
        // concurrente qui laisserait croire à un second contrat inexistant.
        if (request.categorie() == EvenementCategorie.CONTRAT) {
            throw new IllegalArgumentException("La catégorie CONTRAT est générée automatiquement, elle ne peut pas être saisie manuellement.");
        }

        EvenementHistorique evenement = EvenementHistorique.builder()
                .employeId(employeId)
                .titre(request.titre())
                .description(request.description())
                .categorie(request.categorie())
                .statutBadge(request.statutBadge() != null ? request.statutBadge() : StatutBadge.INFO)
                .saisiParId(saisiParId)
                .dateEvenement(request.dateEvenement())
                .build();
        return evenementRepository.save(evenement);
    }
}