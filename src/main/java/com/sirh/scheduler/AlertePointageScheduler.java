package com.sirh.scheduler;

import com.sirh.service.AlertePointageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Module 11, règle 1 : "la tâche CRON scanne la table des pointages chaque
 * dimanche soir." Orchestration uniquement — le calcul vit dans
 * AlertePointageService (règle d'or du plan Lot 8).
 */
@Component
public class AlertePointageScheduler {

    private final AlertePointageService alertePointageService;

    public AlertePointageScheduler(AlertePointageService alertePointageService) {
        this.alertePointageService = alertePointageService;
    }

    @Scheduled(cron = "0 0 20 * * SUN")
    public void executer() {
        alertePointageService.detecterOublisBadgeageSemaine(LocalDate.now());
    }
}
