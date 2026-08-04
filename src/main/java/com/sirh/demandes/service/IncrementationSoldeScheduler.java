package com.sirh.demandes.service;

import com.sirh.demandes.entity.HistoriqueSolde;
import com.sirh.demandes.repository.HistoriqueSoldeRepository;
import com.sirh.parametres.service.ParametreGlobalService;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Incrémentation mensuelle automatique du solde de congés (cahier-fonctionnel.md
 * Module 8). Le taux n'est jamais codé en dur : relu depuis parametres_globaux
 * (clé TAUX_INCREMENTATION_CONGES_MENSUEL, Lot 5) à chaque exécution, pour
 * qu'un changement décidé par le RH s'applique dès le prochain passage sans
 * redéploiement.
 */
@Component
public class IncrementationSoldeScheduler {

    private static final Logger log = LoggerFactory.getLogger(IncrementationSoldeScheduler.class);
    private static final String CLE_TAUX = "TAUX_INCREMENTATION_CONGES_MENSUEL";

    private final EmployeRepository employeRepository;
    private final HistoriqueSoldeRepository historiqueSoldeRepository;
    private final ParametreGlobalService parametreGlobalService;

    public IncrementationSoldeScheduler(EmployeRepository employeRepository,
                                         HistoriqueSoldeRepository historiqueSoldeRepository,
                                         ParametreGlobalService parametreGlobalService) {
        this.employeRepository = employeRepository;
        this.historiqueSoldeRepository = historiqueSoldeRepository;
        this.parametreGlobalService = parametreGlobalService;
    }

    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void incrementerSoldesMensuels() {
        BigDecimal taux = parametreGlobalService.valeurDecimale(CLE_TAUX);
        List<Employe> employesActifs = employeRepository.findByActifTrue();
        log.info("Incrémentation mensuelle des congés : {} employé(s) actif(s), taux = {} j/mois", employesActifs.size(), taux);

        LocalDate aujourdhui = LocalDate.now();
        for (Employe employe : employesActifs) {
            BigDecimal soldeAvant = employe.getSoldeCongesJours() != null ? employe.getSoldeCongesJours() : BigDecimal.ZERO;
            BigDecimal soldeApres = soldeAvant.add(taux);
            employe.setSoldeCongesJours(soldeApres);
            employeRepository.save(employe);

            historiqueSoldeRepository.save(HistoriqueSolde.builder()
                    .employeId(employe.getId())
                    .dateMouvement(aujourdhui)
                    .deltaJours(taux)
                    .soldeApres(soldeApres)
                    .motif("INCREMENT_MENSUEL")
                    .sourceType("CRON_MENSUEL")
                    .build());
        }
    }
}
