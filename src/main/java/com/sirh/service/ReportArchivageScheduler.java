package com.sirh.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Module 7 (CDF) : "les fichiers générés ... sont conservés 12 mois sur le
// serveur principal, puis transférés automatiquement vers un serveur
// d'archivage dédié." cahier-technique.md §1.2 corrige ce point : un seul
// point de montage réseau avec un sous-dossier /archives/, pas un second
// serveur isolé — en cas de panne du montage, on annule/retente 3 fois puis on
// émet un rapport d'erreur d'infrastructure, sans jamais corrompre la base
// active (l'archivage ne touche que des fichiers, jamais les lignes async_jobs).
@Component
@RequiredArgsConstructor
public class ReportArchivageScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReportArchivageScheduler.class);
  private static final long RETENTION_JOURS = 365;
  private static final int TENTATIVES_MAX = 3;

  private final ReportStorage reportStorage;

  @Scheduled(cron = "0 0 3 1 * *")
  public void archiverRapportsAnciens() {
    List<Path> aArchiver;
    try {
      aArchiver = reportStorage.listerRapportsAArchiver(RETENTION_JOURS);
    } catch (IOException e) {
      log.error(
          "Archivage mensuel des rapports : montage réseau inaccessible, annulation sans impact sur"
              + " la base active.",
          e);
      return;
    }

    for (Path fichier : aArchiver) {
      archiverAvecRetry(fichier);
    }
  }

  private void archiverAvecRetry(Path fichier) {
    for (int tentative = 1; tentative <= TENTATIVES_MAX; tentative++) {
      try {
        reportStorage.archiver(fichier);
        return;
      } catch (IOException e) {
        if (tentative == TENTATIVES_MAX) {
          log.error(
              "Échec de l'archivage de {} après {} tentatives — rapport d'erreur d'infrastructure, "
                  + "aucune suppression effectuée, la base active n'est pas impactée.",
              fichier,
              TENTATIVES_MAX,
              e);
        }
      }
    }
  }
}
