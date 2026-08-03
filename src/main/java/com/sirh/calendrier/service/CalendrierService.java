package com.sirh.calendrier.service;

import com.sirh.calendrier.entity.JourFerie;
import com.sirh.calendrier.entity.TypeJourFerie;
import com.sirh.calendrier.exception.JourFerieNotFoundException;
import com.sirh.calendrier.repository.JourFerieRepository;
import com.sirh.demandes.entity.HistoriqueSolde;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.HistoriqueSoldeRepository;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    public List<JourFerie> findByPeriode(LocalDate debut, LocalDate fin) {
        return jourFerieRepository.findByDateBetweenOrderByDate(debut, fin);
    }

    @Transactional
    public JourFerie creer(LocalDate date, String libelle, TypeJourFerie type, Boolean national, String region) {
        JourFerie jourFerie = jourFerieRepository.save(JourFerie.builder()
                .date(date).libelle(libelle).type(type)
                .national(national != null ? national : true)
                .region(region)
                .build());
        recrediterChevauchements(jourFerie);
        return jourFerie;
    }

    // Rollback total au 1er échec (même principe que l'import Excel pointages,
    // cf. CLAUDE.md) : une seule date déjà présente ou invalide fait échouer tout
    // le lot, aucune insertion partielle.
    @Transactional
    public List<JourFerie> importer(List<JourFerie> aImporter) {
        return aImporter.stream()
                .map(j -> {
                    JourFerie sauvegarde = jourFerieRepository.save(j);
                    recrediterChevauchements(sauvegarde);
                    return sauvegarde;
                })
                .toList();
    }

    // Pas de nouveau recrédit ici : la règle (cahier-fonctionnel.md Module 6) se
    // déclenche quand le férié "est validé", c'est-à-dire à sa création — une
    // correction ultérieure (libellé, région) ne doit pas recréditer une 2e fois
    // les employés déjà traités lors de la création initiale.
    @Transactional
    public JourFerie modifier(Long id, LocalDate date, String libelle, TypeJourFerie type, Boolean national, String region) {
        JourFerie jourFerie = jourFerieRepository.findById(id).orElseThrow(() -> new JourFerieNotFoundException(id));
        jourFerie.setDate(date);
        jourFerie.setLibelle(libelle);
        jourFerie.setType(type);
        jourFerie.setNational(national != null ? national : true);
        jourFerie.setRegion(region);
        return jourFerieRepository.save(jourFerie);
    }

    @Transactional
    public void supprimer(Long id) {
        if (!jourFerieRepository.existsById(id)) {
            throw new JourFerieNotFoundException(id);
        }
        jourFerieRepository.deleteById(id);
    }

    // Seul le type FERIE (férié, y compris mobile) déclenche le recrédit : une
    // FERMETURE_COLLECTIVE est une décision RH interne, pas un jour chômé légal
    // prioritaire sur un congé déjà posé (cahier-fonctionnel.md Module 6).
    private void recrediterChevauchements(JourFerie jourFerie) {
        if (jourFerie.getType() != TypeJourFerie.FERIE) {
            return;
        }
        List<Indisponibilite> congesChevauchants = indisponibiliteRepository
                .findByTypeIndisponibiliteAndStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
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
}
