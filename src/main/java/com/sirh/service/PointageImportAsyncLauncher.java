package com.sirh.service;

import com.sirh.exception.ConflictException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bean séparé de ImportPointagesService : @Async ne fonctionne que sur un appel externe passant par
 * le proxy Spring. Si ImportPointagesService s'appelait lui-même (this.traiterAsync(...)),
 * l'annotation serait ignorée et le traitement s'exécuterait de façon synchrone sur le thread de la
 * requête HTTP — exactement ce qu'on veut éviter ici.
 */
@Service
@RequiredArgsConstructor
public class PointageImportAsyncLauncher {

  private static final Logger log = LoggerFactory.getLogger(PointageImportAsyncLauncher.class);

  private final AsyncJobService asyncJobService;
  private final PointageImportWriter pointageImportWriter;
  private final MessageSource messageSource;

  @Async("pointagesImportExecutor")
  public void traiterAsync(UUID jobReference, byte[] contenu) {
    asyncJobService.marquerEnCoursByReference(jobReference);
    try {
      int nombreLignesTraitees = pointageImportWriter.importer(contenu);
      asyncJobService.marquerSuccesByReference(jobReference, nombreLignesTraitees);
    } catch (ConflictException e) {
      Integer ligne = (Integer) e.getArgs()[0];
      String message =
          messageSource.getMessage(
              e.getErrorCode().name(), e.getArgs(), LocaleContextHolder.getLocale());
      log.warn("Import pointages job {} rejeté ligne {} : {}", jobReference, ligne, message);
      asyncJobService.marquerEchecByReference(jobReference, ligne, message);
    } catch (Exception e) {
      log.error("Import pointages job {} en échec inattendu", jobReference, e);
      asyncJobService.marquerEchecByReference(
          jobReference, null, "Erreur inattendue : " + e.getMessage());
    }
  }
}
