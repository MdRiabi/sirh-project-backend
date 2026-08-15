package com.sirh.scheduler;

import com.sirh.service.SousActiviteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Module 11, règle 2 : "la tâche CRON scanne la table le dernier jour du
 * mois." Spring @Scheduled (cron 6 champs) n'a pas d'équivalent au "L" de
 * Quartz : on se déclenche chaque jour entre le 28 et le 31, et on ne délègue
 * au calcul (SousActiviteService) que si le jour du run est bien le dernier
 * du mois — seule logique tolérée ici, l'orchestration reste la règle d'or.
 */
@Component
public class AlerteSousActiviteScheduler {

    private final SousActiviteService sousActiviteService;

    public AlerteSousActiviteScheduler(SousActiviteService sousActiviteService) {
        this.sousActiviteService = sousActiviteService;
    }

    @Scheduled(cron = "0 0 23 28-31 * *")
    public void executer() {
        LocalDate aujourdhui = LocalDate.now();
        if (aujourdhui.getDayOfMonth() == aujourdhui.lengthOfMonth()) {
            sousActiviteService.detecterSousActivite(aujourdhui);
        }
    }
}
