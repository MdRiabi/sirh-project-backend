package com.sirh.service;

import com.sirh.service.AsyncJobService;
import com.sirh.exception.ImportPointagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bean séparé de ImportPointagesService : @Async ne fonctionne que sur un
 * appel externe passant par le proxy Spring. Si ImportPointagesService
 * s'appelait lui-même (this.traiterAsync(...)), l'annotation serait ignorée
 * et le traitement s'exécuterait de façon synchrone sur le thread de la
 * requête HTTP — exactement ce qu'on veut éviter ici.
 */
@Service
public class PointageImportAsyncLauncher {

    private static final Logger log = LoggerFactory.getLogger(PointageImportAsyncLauncher.class);

    private final AsyncJobService asyncJobService;
    private final PointageImportWriter pointageImportWriter;

    public PointageImportAsyncLauncher(AsyncJobService asyncJobService, PointageImportWriter pointageImportWriter) {
        this.asyncJobService = asyncJobService;
        this.pointageImportWriter = pointageImportWriter;
    }

    @Async("pointagesImportExecutor")
    public void traiterAsync(Long jobId, byte[] contenu) {
        asyncJobService.marquerEnCours(jobId);
        try {
            int nombreLignesTraitees = pointageImportWriter.importer(contenu);
            asyncJobService.marquerSucces(jobId, nombreLignesTraitees);
        } catch (ImportPointagesException e) {
            log.warn("Import pointages job {} rejeté ligne {} : {}", jobId, e.getLigne(), e.getMessage());
            asyncJobService.marquerEchec(jobId, e.getLigne(), e.getMessage());
        } catch (Exception e) {
            log.error("Import pointages job {} en échec inattendu", jobId, e);
            asyncJobService.marquerEchec(jobId, null, "Erreur inattendue : " + e.getMessage());
        }
    }
}