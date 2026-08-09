package com.sirh.reporting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// cahier-fonctionnel.md Module 7 : purge/archivage mensuelle, retry 3x en cas
// de panne du montage réseau, jamais de corruption de la base active (aucun
// appel à AsyncJobService ici — l'archivage ne touche que des fichiers).
@ExtendWith(MockitoExtension.class)
class ReportArchivageSchedulerTest {

    @Mock
    private ReportStorage reportStorage;

    private ReportArchivageScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReportArchivageScheduler(reportStorage);
    }

    @Test
    void archiverRapportsAnciens_archiveChaqueFichierEligible() throws IOException {
        Path fichier1 = Path.of("rapport1.pdf");
        Path fichier2 = Path.of("rapport2.xlsx");
        when(reportStorage.listerRapportsAArchiver(365)).thenReturn(List.of(fichier1, fichier2));

        scheduler.archiverRapportsAnciens();

        verify(reportStorage).archiver(fichier1);
        verify(reportStorage).archiver(fichier2);
    }

    @Test
    void archiverRapportsAnciens_reessaieTroisFoisPuisAbandonneSansException() throws IOException {
        Path fichier = Path.of("rapport-en-echec.pdf");
        when(reportStorage.listerRapportsAArchiver(365)).thenReturn(List.of(fichier));
        doThrow(new IOException("montage réseau inaccessible")).when(reportStorage).archiver(fichier);

        scheduler.archiverRapportsAnciens();

        verify(reportStorage, times(3)).archiver(fichier);
    }

    @Test
    void archiverRapportsAnciens_reussitApresUnEchecTransitoire() throws IOException {
        Path fichier = Path.of("rapport-flaky.pdf");
        when(reportStorage.listerRapportsAArchiver(365)).thenReturn(List.of(fichier));
        doThrow(new IOException("timeout")).doNothing().when(reportStorage).archiver(fichier);

        scheduler.archiverRapportsAnciens();

        verify(reportStorage, times(2)).archiver(fichier);
    }

    @Test
    void archiverRapportsAnciens_montageInaccessibleALaListe_neLeveRien() throws IOException {
        when(reportStorage.listerRapportsAArchiver(365)).thenThrow(new IOException("montage réseau inaccessible"));

        scheduler.archiverRapportsAnciens();

        verify(reportStorage, never()).archiver(any());
    }
}
