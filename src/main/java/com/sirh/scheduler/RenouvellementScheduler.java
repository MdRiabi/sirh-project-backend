package com.sirh.scheduler;

import com.sirh.service.RenouvellementService;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Module 12 : détection quotidienne des contrats CDD atteignant M+5/M+11 et bascule des tâches
 * EN_COURS dépassées en URGENT_EXPIRE. Orchestration uniquement — le calcul vit dans
 * RenouvellementService (règle d'or du plan Lot 8, reconduite ici).
 */
@Component
public class RenouvellementScheduler {

  private final RenouvellementService renouvellementService;

  public RenouvellementScheduler(RenouvellementService renouvellementService) {
    this.renouvellementService = renouvellementService;
  }

  @Scheduled(cron = "0 0 6 * * *")
  public void executer() {
    renouvellementService.genererEtEscalader(LocalDate.now());
  }
}
