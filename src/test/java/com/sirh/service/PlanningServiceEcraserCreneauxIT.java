package com.sirh.service;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Planning;
import com.sirh.model.enums.PlanningStatut;
import com.sirh.model.entity.Shift;
import com.sirh.model.repository.PlanningRepository;
import com.sirh.model.repository.ShiftRepository;
import com.sirh.service.PlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre PlanningService.ecraserCreneaux (Lot 5) : source de vérité unique du
 * planning à la validation d'une demande (cahier-fonctionnel.md Modules 8/13,
 * "le planning est mis à jour") — appelé par com.sirh.demandes, pas exposé
 * en HTTP direct, d'où un test au niveau service plutôt que contrôleur.
 */
class PlanningServiceEcraserCreneauxIT extends AbstractIntegrationTest {

    @Autowired
    private PlanningService planningService;
    @Autowired
    private PlanningRepository planningRepository;
    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    void ecraserCreneaux_supprimeAffectationExistanteEtInsereLeNouveauStatut() {
        Employe employe = createFixtureEmploye();
        Shift matin = shiftRepository.save(Shift.builder()
                .nom("Matin").typeRotation("FIXE").heureDebut(LocalTime.of(6, 0)).heureFin(LocalTime.of(14, 0)).build());
        LocalDate jour = LocalDate.now().plusDays(50);

        Planning existant = planningRepository.save(Planning.builder()
                .employeId(employe.getId()).shiftId(matin.getId())
                .dateDebut(jour.atTime(6, 0)).dateFin(jour.atTime(14, 0))
                .statut(PlanningStatut.VALIDE)
                .build());

        Planning conge = planningService.ecraserCreneaux(
                employe.getId(), jour.atStartOfDay(), jour.plusDays(1).atStartOfDay(), PlanningStatut.CONGE, "Congé payé validé");

        assertThat(planningRepository.findById(existant.getId())).isEmpty();
        assertThat(conge.getStatut()).isEqualTo(PlanningStatut.CONGE);
        List<Planning> restants = planningRepository
                .findByEmployeIdAndDateDebutBetweenAndDeletedAtIsNullOrderByDateDebut(employe.getId(), jour.atStartOfDay(), jour.plusDays(1).atStartOfDay());
        assertThat(restants).hasSize(1);
        assertThat(restants.get(0).getStatut()).isEqualTo(PlanningStatut.CONGE);
    }

    @Test
    void ecraserCreneaux_reutiliseLeMemeShiftSysteme_surDeuxAppels() {
        Employe employe = createFixtureEmploye();
        LocalDate jour = LocalDate.now().plusDays(51);

        Planning premier = planningService.ecraserCreneaux(
                employe.getId(), jour.atTime(14, 0), jour.atTime(16, 0), PlanningStatut.ABSENCE, "Retard justifié");
        Planning second = planningService.ecraserCreneaux(
                employe.getId(), jour.plusDays(5).atStartOfDay(), jour.plusDays(6).atStartOfDay(), PlanningStatut.CONGE, "Congé");

        assertThat(premier.getShiftId()).isEqualTo(second.getShiftId());
    }
}
