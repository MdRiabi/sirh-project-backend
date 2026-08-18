package com.sirh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sirh.model.entity.Employe;
import com.sirh.model.entity.HistoriqueSolde;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.HistoriqueSoldeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Couvre IncrementationSoldeScheduler (Module 8, Lot 5) : le taux ne doit jamais être codé en dur —
 * relu depuis parametres_globaux à chaque exécution — et chaque employé actif reçoit un mouvement
 * historique_soldes tracé (motif INCREMENT_MENSUEL).
 */
@ExtendWith(MockitoExtension.class)
class IncrementationSoldeSchedulerTest {

  @Mock private EmployeRepository employeRepository;
  @Mock private HistoriqueSoldeRepository historiqueSoldeRepository;
  @Mock private ParametreGlobalService parametreGlobalService;

  @Test
  void incrementerSoldesMensuels_appliqueLeTauxLuDynamiquementAChaqueEmployeActif() {
    when(parametreGlobalService.valeurDecimale("TAUX_INCREMENTATION_CONGES_MENSUEL"))
        .thenReturn(BigDecimal.valueOf(1.5));

    Employe employe1 =
        Employe.builder()
            .id(UUID.randomUUID())
            .soldeCongesJours(BigDecimal.valueOf(10))
            .actif(true)
            .build();
    Employe employe2 =
        Employe.builder().id(UUID.randomUUID()).soldeCongesJours(null).actif(true).build();
    when(employeRepository.findByActifTrueAndDeletedAtIsNull())
        .thenReturn(List.of(employe1, employe2));

    IncrementationSoldeScheduler scheduler =
        new IncrementationSoldeScheduler(
            employeRepository, historiqueSoldeRepository, parametreGlobalService);
    scheduler.incrementerSoldesMensuels();

    assertThat(employe1.getSoldeCongesJours()).isEqualByComparingTo("11.5");
    assertThat(employe2.getSoldeCongesJours()).isEqualByComparingTo("1.5");

    ArgumentCaptor<HistoriqueSolde> captor = ArgumentCaptor.forClass(HistoriqueSolde.class);
    verify(historiqueSoldeRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(
            h -> {
              assertThat(h.getMotif()).isEqualTo("INCREMENT_MENSUEL");
              assertThat(h.getSourceType()).isEqualTo("CRON_MENSUEL");
              assertThat(h.getDeltaJours()).isEqualByComparingTo("1.5");
              assertThat(h.getDateMouvement()).isEqualTo(LocalDate.now());
            });
  }

  @Test
  void incrementerSoldesMensuels_relitLeTauxAChaqueAppel_pasDeCache() {
    when(parametreGlobalService.valeurDecimale(any())).thenReturn(BigDecimal.valueOf(2));
    when(employeRepository.findByActifTrueAndDeletedAtIsNull()).thenReturn(List.of());

    IncrementationSoldeScheduler scheduler =
        new IncrementationSoldeScheduler(
            employeRepository, historiqueSoldeRepository, parametreGlobalService);
    scheduler.incrementerSoldesMensuels();
    scheduler.incrementerSoldesMensuels();

    verify(parametreGlobalService, times(2)).valeurDecimale("TAUX_INCREMENTATION_CONGES_MENSUEL");
  }
}
