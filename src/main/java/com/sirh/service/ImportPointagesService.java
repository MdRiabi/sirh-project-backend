package com.sirh.service;

import com.sirh.model.entity.AsyncJob;
import com.sirh.model.enums.AsyncJobType;
import com.sirh.service.AsyncJobService;
import com.sirh.exception.ImportPointagesException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Point d'entrée synchrone de l'import Excel (cahier-fonctionnel.md Module 1 :
 * "Règle d'importation stricte", ≤1500 lignes). Valide et crée le job, puis
 * délègue le traitement asynchrone à PointageImportAsyncLauncher (bean
 * séparé — cf. sa Javadoc pour la raison : self-invocation et @Async).
 */
@Service
public class ImportPointagesService {

    private static final int MAX_LIGNES = 1500;

    private final AsyncJobService asyncJobService;
    private final PointageImportAsyncLauncher asyncLauncher;

    public ImportPointagesService(AsyncJobService asyncJobService, PointageImportAsyncLauncher asyncLauncher) {
        this.asyncJobService = asyncJobService;
        this.asyncLauncher = asyncLauncher;
    }

    public AsyncJob creerJob(MultipartFile fichier, Long executeParId) {
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Fichier illisible.", e);
        }
        validerNombreDeLignes(contenu);

        AsyncJob job = asyncJobService.creer(AsyncJobType.IMPORT_POINTAGES, fichier.getOriginalFilename(), executeParId);
        asyncLauncher.traiterAsync(job.getId(), contenu);
        return job;
    }

    private void validerNombreDeLignes(byte[] contenu) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenu))) {
            Sheet sheet = workbook.getSheetAt(0);
            int nombreLignesDonnees = sheet.getLastRowNum() - sheet.getFirstRowNum(); // exclut l'en-tête
            if (nombreLignesDonnees > MAX_LIGNES) {
                throw new ImportPointagesException(0,
                        "Fichier trop volumineux : " + nombreLignesDonnees + " lignes (maximum " + MAX_LIGNES + ").");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Fichier Excel illisible.", e);
        }
    }
}