package com.sirh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// cahier-technique.md §1.2 : point de montage unique avec sous-dossiers
// /reports/ et /archives/ — partagé par ReportGenerationService (écriture) et
// ReportArchivageScheduler (purge mensuelle à 12 mois, Module 7 du CDF).
@Component
public class ReportStorage {

    private final Path reportsDir;
    private final Path archivesDir;

    public ReportStorage(@Value("${storage.path}") String storagePath) {
        Path storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.reportsDir = storageRoot.resolve("reports");
        this.archivesDir = storageRoot.resolve("archives");
    }

    public void ecrire(String nomFichier, byte[] contenu) throws IOException {
        Files.createDirectories(reportsDir);
        Files.write(reportsDir.resolve(nomFichier), contenu);
    }

    // Le fichier peut être soit dans reports/ (généré récemment), soit dans
    // archives/ (déplacé par la purge mensuelle) — le job async_jobs ne garde
    // que le nom de fichier, jamais le dossier courant (cf. terminerJobAvecSucces).
    public Path resoudrePourTelechargement(String nomFichier) {
        Path dansReports = reportsDir.resolve(nomFichier).normalize();
        if (dansReports.startsWith(reportsDir) && Files.exists(dansReports)) {
            return dansReports;
        }
        Path dansArchives = archivesDir.resolve(nomFichier).normalize();
        if (dansArchives.startsWith(archivesDir) && Files.exists(dansArchives)) {
            return dansArchives;
        }
        return null;
    }

    public List<Path> listerRapportsAArchiver(long ageMinimalJours) throws IOException {
        if (!Files.isDirectory(reportsDir)) {
            return List.of();
        }
        long seuil = System.currentTimeMillis() - ageMinimalJours * 24L * 60 * 60 * 1000;
        try (Stream<Path> fichiers = Files.list(reportsDir)) {
            return fichiers.filter(Files::isRegularFile)
                    .filter(p -> dernierModifieAvant(p, seuil))
                    .toList();
        }
    }

    private boolean dernierModifieAvant(Path path, long seuilMillis) {
        try {
            return Files.getLastModifiedTime(path).toMillis() < seuilMillis;
        } catch (IOException e) {
            return false;
        }
    }

    public void archiver(Path fichierSource) throws IOException {
        Files.createDirectories(archivesDir);
        Files.move(fichierSource, archivesDir.resolve(fichierSource.getFileName()),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
