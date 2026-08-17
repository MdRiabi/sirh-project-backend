package com.sirh.service;

import com.sirh.exception.ResourceNotFoundException;
import com.sirh.exception.ErrorCode;

import com.sirh.model.dto.JourFerieDto;
import com.sirh.model.entity.JourFerie;
import com.sirh.model.enums.TypeJourFerie;
import com.sirh.model.repository.JourFerieRepository;
import com.sirh.model.entity.HistoriqueSolde;
import com.sirh.model.entity.Indisponibilite;
import com.sirh.model.repository.HistoriqueSoldeRepository;
import com.sirh.model.repository.IndisponibiliteRepository;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CRUD du référentiel jours fériés / fermetures collectives, avec recrédit
 * automatique de solde (cahier-fonctionnel.md Module 6) : si un jour férié
 * (mobile ou fixe) tombe sur une période de congé déjà VALIDE, le férié est
 * prioritaire et le jour de congé est recrédité à l'employé.
 */
@Service
public class CalendrierService {

    private final JourFerieRepository jourFerieRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final HistoriqueSoldeRepository historiqueSoldeRepository;
    private final EmployeRepository employeRepository;

    public CalendrierService(JourFerieRepository jourFerieRepository,
                              IndisponibiliteRepository indisponibiliteRepository,
                              HistoriqueSoldeRepository historiqueSoldeRepository,
                              EmployeRepository employeRepository) {
        this.jourFerieRepository = jourFerieRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.historiqueSoldeRepository = historiqueSoldeRepository;
        this.employeRepository = employeRepository;
    }

    @Transactional(readOnly = true)
    public List<JourFerieDto> findByPeriode(LocalDate debut, LocalDate fin) {
        return jourFerieRepository.findByDateBetweenAndDeletedAtIsNullOrderByDate(debut, fin)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public JourFerieDto creer(LocalDate date, String libelle, TypeJourFerie type, Boolean national, String region) {
        JourFerie jourFerie = jourFerieRepository.save(JourFerie.builder()
                .date(date).libelle(libelle).type(type)
                .national(national != null ? national : true)
                .region(region)
                .build());
        recrediterChevauchements(jourFerie);
        return toDto(jourFerie);
    }

    // Rollback total au 1er échec (même principe que l'import Excel pointages,
    // cf. CLAUDE.md) : une seule date déjà présente ou invalide fait échouer tout
    // le lot, aucune insertion partielle.
    @Transactional
    public List<JourFerieDto> importer(List<JourFerie> aImporter) {
        return aImporter.stream()
                .map(j -> {
                    JourFerie sauvegarde = jourFerieRepository.save(j);
                    recrediterChevauchements(sauvegarde);
                    return toDto(sauvegarde);
                })
                .toList();
    }

    // Pas de nouveau recrédit ici : la règle (cahier-fonctionnel.md Module 6) se
    // déclenche quand le férié "est validé", c'est-à-dire à sa création — une
    // correction ultérieure (libellé, région) ne doit pas recréditer une 2e fois
    // les employés déjà traités lors de la création initiale.
    @Transactional
    public JourFerieDto modifierJourFerieByReference(UUID reference, LocalDate date, String libelle, TypeJourFerie type, Boolean national, String region) {
        JourFerie jourFerie = getJourFerieByReference(reference);
        jourFerie.setDate(date);
        jourFerie.setLibelle(libelle);
        jourFerie.setType(type);
        jourFerie.setNational(national != null ? national : true);
        jourFerie.setRegion(region);
        return toDto(jourFerieRepository.save(jourFerie));
    }

    @Transactional
    public void supprimerJourFerieByReference(UUID reference) {
        jourFerieRepository.delete(getJourFerieByReference(reference));
    }

    private JourFerie getJourFerieByReference(UUID reference) {
        return jourFerieRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.JOUR_FERIE_NOT_FOUND, new Object[]{reference}));
    }

    // Seul le type FERIE (férié, y compris mobile) déclenche le recrédit : une
    // FERMETURE_COLLECTIVE est une décision RH interne, pas un jour chômé légal
    // prioritaire sur un congé déjà posé (cahier-fonctionnel.md Module 6).
    private void recrediterChevauchements(JourFerie jourFerie) {
        if (jourFerie.getType() != TypeJourFerie.FERIE) {
            return;
        }
        List<Indisponibilite> congesChevauchants = indisponibiliteRepository
                .findByTypeIndisponibiliteAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualAndDeletedAtIsNull(
                        "CONGE_PAYE", "VALIDE", jourFerie.getDate(), jourFerie.getDate());

        for (Indisponibilite conge : congesChevauchants) {
            Employe employe = employeRepository.findById(conge.getEmployeId()).orElse(null);
            if (employe == null) {
                continue;
            }
            BigDecimal soldeAvant = employe.getSoldeCongesJours() != null ? employe.getSoldeCongesJours() : BigDecimal.ZERO;
            BigDecimal soldeApres = soldeAvant.add(BigDecimal.ONE);
            employe.setSoldeCongesJours(soldeApres);
            employeRepository.save(employe);

            historiqueSoldeRepository.save(HistoriqueSolde.builder()
                    .employeId(employe.getId())
                    .dateMouvement(jourFerie.getDate())
                    .deltaJours(BigDecimal.ONE)
                    .soldeApres(soldeApres)
                    .motif("RECREDIT_FERIE")
                    .sourceType("JOUR_FERIE")
                    .sourceId(jourFerie.getId())
                    .build());
        }
    }

    private JourFerieDto toDto(JourFerie j) {
        return new JourFerieDto(j.getReference(), j.getDate(), j.getLibelle(), j.getType(), j.getNational(), j.getRegion());
    }
}
