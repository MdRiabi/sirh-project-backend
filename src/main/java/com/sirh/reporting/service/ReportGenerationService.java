package com.sirh.reporting.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sirh.async.dto.AsyncJobDto;
import com.sirh.async.entity.AsyncJob;
import com.sirh.async.entity.AsyncJobStatut;
import com.sirh.async.entity.AsyncJobType;
import com.sirh.async.exception.AsyncJobNotFoundException;
import com.sirh.async.service.AsyncJobService;
import com.sirh.reporting.exception.ReportNotReadyException;
import com.sirh.demandes.entity.Indisponibilite;
import com.sirh.demandes.repository.IndisponibiliteRepository;
import com.sirh.personnel.entity.Departement;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.DepartementRepository;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.entity.PointageStatut;
import com.sirh.pointages.repository.PointageBrutRepository;
import com.sirh.reporting.dto.RapportGenere;
import com.sirh.reporting.dto.ReportFormat;
import com.sirh.reporting.dto.ReportGenererRequest;
import com.sirh.reporting.dto.ReportGenerationResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

// Module 7 (CDF) : "Si le calcul prend < 5 secondes, le fichier est
// téléchargé directement. Si le calcul est lourd (> 5 secondes), le système
// lance une tâche asynchrone et prévient l'utilisateur." — la génération est
// toujours soumise au pool reportExecutor ; on attend jusqu'à 5s le résultat,
// au-delà on bascule sur le suivi async_jobs (même mécanisme que l'import
// Excel du Lot 2) pendant que le calcul continue en arrière-plan.
@Service
public class ReportGenerationService {

    private final long delaiSynchroneMs;
    private final EmployeRepository employeRepository;
    private final DepartementRepository departementRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final PointageBrutRepository pointageBrutRepository;
    private final AsyncJobService asyncJobService;
    private final Executor reportExecutor;
    private final ReportStorage reportStorage;

    public ReportGenerationService(EmployeRepository employeRepository,
                                    DepartementRepository departementRepository,
                                    IndisponibiliteRepository indisponibiliteRepository,
                                    PointageBrutRepository pointageBrutRepository,
                                    AsyncJobService asyncJobService,
                                    @org.springframework.beans.factory.annotation.Qualifier("reportExecutor") Executor reportExecutor,
                                    ReportStorage reportStorage,
                                    // Configurable uniquement pour les tests (forcer la bascule
                                    // asynchrone sans dépendre d'un calcul réellement lent) —
                                    // 5000ms par défaut, conforme au CDF Module 7 en production.
                                    @org.springframework.beans.factory.annotation.Value("${reporting.delai-synchrone-ms:5000}") long delaiSynchroneMs) {
        this.employeRepository = employeRepository;
        this.departementRepository = departementRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.pointageBrutRepository = pointageBrutRepository;
        this.asyncJobService = asyncJobService;
        this.delaiSynchroneMs = delaiSynchroneMs;
        this.reportExecutor = reportExecutor;
        this.reportStorage = reportStorage;
    }

    public ReportGenerationResult genererRapport(ReportGenererRequest request, Long executeParId) {
        CompletableFuture<RapportGenere> futur = CompletableFuture.supplyAsync(
                () -> construire(request), reportExecutor);

        try {
            RapportGenere rapport = futur.get(delaiSynchroneMs, TimeUnit.MILLISECONDS);
            return new ReportGenerationResult.Pret(rapport);
        } catch (TimeoutException e) {
            AsyncJob job = asyncJobService.creer(AsyncJobType.REPORT, nomFichierAttendu(request), executeParId);
            Long jobId = job.getId();
            futur.thenAccept(rapport -> terminerJobAvecSucces(jobId, rapport))
                    .exceptionally(ex -> {
                        asyncJobService.marquerEchec(jobId, null, ex.getMessage());
                        return null;
                    });
            return new ReportGenerationResult.Lance(AsyncJobDto.from(job));
        } catch (Exception e) {
            throw new IllegalStateException("Échec de la génération du rapport.", e);
        }
    }

    // GET /reports/tasks/{taskId} : isolé des autres types de async_jobs (ex.
    // IMPORT_POINTAGES) pour ne jamais exposer un job hors périmètre reporting.
    public AsyncJobDto obtenirTache(Long taskId) {
        AsyncJob job = asyncJobService.findById(taskId);
        if (job.getType() != AsyncJobType.REPORT) {
            throw new AsyncJobNotFoundException(taskId);
        }
        return AsyncJobDto.from(job);
    }

    public record FichierTelecharge(Resource resource, String nomFichier, MediaType contentType) {
    }

    public FichierTelecharge chargerFichierResultat(Long taskId) {
        AsyncJob job = asyncJobService.findById(taskId);
        if (job.getType() != AsyncJobType.REPORT) {
            throw new AsyncJobNotFoundException(taskId);
        }
        if (job.getStatut() != AsyncJobStatut.SUCCESS || job.getResultatUrl() == null) {
            throw new ReportNotReadyException(taskId);
        }
        String nomFichier = job.getResultatUrl();
        var chemin = reportStorage.resoudrePourTelechargement(nomFichier);
        if (chemin == null) {
            throw new ReportNotReadyException(taskId);
        }
        MediaType contentType = nomFichier.endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return new FichierTelecharge(new FileSystemResource(chemin), nomFichier, contentType);
    }

    // resultatUrl porte le nom du fichier physique sous storage/reports/ (ou
    // storage/archives/ après la purge mensuelle, cf. ReportArchivageScheduler)
    // — le lien de téléchargement effectif est toujours GET /reports/tasks/{id}/download,
    // qui résout ce nom de fichier indépendamment de son dossier courant.
    private void terminerJobAvecSucces(Long jobId, RapportGenere rapport) {
        try {
            reportStorage.ecrire(rapport.nomFichier(), rapport.contenu());
            asyncJobService.marquerSuccesAvecResultat(jobId, rapport.nomFichier());
        } catch (IOException e) {
            asyncJobService.marquerEchec(jobId, null, "Échec de l'écriture du rapport sur le stockage : " + e.getMessage());
        }
    }

    private String nomFichierAttendu(ReportGenererRequest request) {
        String extension = request.format() == ReportFormat.PDF ? ".pdf" : ".xlsx";
        return request.type().name().toLowerCase() + "-" + request.annee() + "-" + UUID.randomUUID() + extension;
    }

    private RapportGenere construire(ReportGenererRequest request) {
        return switch (request.type()) {
            case TURNOVER_ANNUEL -> genererTurnover(request);
            case ABSENTEISME_ANNUEL -> genererAbsenteisme(request);
        };
    }

    private RapportGenere genererTurnover(ReportGenererRequest request) {
        int annee = request.annee();
        Long departementId = request.departementId();
        List<Object[]> effectifsBruts = employeRepository.evolutionEffectifsAnnee(annee, departementId);
        List<Object[]> departsBruts = employeRepository.departsAnnee(annee, departementId);

        Map<String, BigDecimal> effectifParMois = effectifsBruts.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> new BigDecimal(r[1].toString())));

        List<String> entetes = List.of("Mois", "Effectif", "Départs", "Taux de turnover");
        List<List<String>> lignes = new ArrayList<>();
        for (Object[] ligne : departsBruts) {
            String mois = (String) ligne[0];
            BigDecimal departsMois = new BigDecimal(ligne[1].toString());
            BigDecimal effectifMois = effectifParMois.getOrDefault(mois, BigDecimal.ONE);
            BigDecimal taux = effectifMois.signum() == 0 ? BigDecimal.ZERO
                    : departsMois.multiply(BigDecimal.valueOf(100)).divide(effectifMois, 1, RoundingMode.HALF_UP);
            lignes.add(List.of(mois, effectifParMois.getOrDefault(mois, BigDecimal.ZERO).toPlainString(),
                    departsMois.toPlainString(), taux + "%"));
        }

        String titre = "Turnover annuel " + annee;
        String nomFichier = "turnover-" + annee + (request.format() == ReportFormat.PDF ? ".pdf" : ".xlsx");
        return render(request.format(), titre, entetes, lignes, nomFichier);
    }

    private RapportGenere genererAbsenteisme(ReportGenererRequest request) {
        int annee = request.annee();
        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        LocalDate finAnnee = LocalDate.of(annee, 12, 31);

        List<Departement> departements = request.departementId() != null
                ? departementRepository.findById(request.departementId()).map(List::of).orElseGet(List::of)
                : departementRepository.findAll();

        List<Indisponibilite> indisponibilitesAnnee = indisponibiliteRepository
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqual("VALIDE", finAnnee, debutAnnee);

        List<Object[]> anomaliesParEmploye = pointageBrutRepository.compterAnomaliesParEmploye(
                debutAnnee.atStartOfDay(), finAnnee.plusDays(1).atStartOfDay(), PointageStatut.ANOMALIE_NON_JUSTIFIEE);
        Map<UUID, Long> anomaliesParEmployeMap = anomaliesParEmploye.stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));

        List<String> entetes = List.of("Département", "Effectif", "Jours d'absence", "Anomalies non justifiées");
        List<List<String>> lignes = new ArrayList<>();
        for (Departement departement : departements) {
            List<Employe> employes = employeRepository.findByDepartement_Id(departement.getId());
            Set<UUID> employeIds = employes.stream().map(Employe::getId).collect(Collectors.toSet());

            long joursAbsence = indisponibilitesAnnee.stream()
                    .filter(i -> employeIds.contains(i.getEmployeId()))
                    .mapToLong(i -> joursChevauchement(i, debutAnnee, finAnnee))
                    .sum();
            long anomalies = anomaliesParEmployeMap.entrySet().stream()
                    .filter(e -> employeIds.contains(e.getKey()))
                    .mapToLong(Map.Entry::getValue)
                    .sum();

            lignes.add(List.of(departement.getNom(), String.valueOf(employes.size()),
                    String.valueOf(joursAbsence), String.valueOf(anomalies)));
        }

        String titre = "Absentéisme annuel " + annee;
        String nomFichier = "absenteisme-" + annee + (request.format() == ReportFormat.PDF ? ".pdf" : ".xlsx");
        return render(request.format(), titre, entetes, lignes, nomFichier);
    }

    private long joursChevauchement(Indisponibilite indisponibilite, LocalDate debutAnnee, LocalDate finAnnee) {
        LocalDate debut = indisponibilite.getDateDebut().isBefore(debutAnnee) ? debutAnnee : indisponibilite.getDateDebut();
        LocalDate fin = indisponibilite.getDateFin().isAfter(finAnnee) ? finAnnee : indisponibilite.getDateFin();
        return ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    private RapportGenere render(ReportFormat format, String titre, List<String> entetes,
                                  List<List<String>> lignes, String nomFichier) {
        return format == ReportFormat.PDF
                ? renderPdf(titre, entetes, lignes, nomFichier)
                : renderExcel(titre, entetes, lignes, nomFichier);
    }

    private RapportGenere renderPdf(String titre, List<String> entetes, List<List<String>> lignes, String nomFichier) {
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph(titre, fontTitre));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(entetes.size());
            table.setWidthPercentage(100);
            Font fontEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String entete : entetes) {
                table.addCell(new PdfPCell(new Phrase(entete, fontEntete)));
            }
            Font fontCellule = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (List<String> ligne : lignes) {
                for (String valeur : ligne) {
                    table.addCell(new PdfPCell(new Phrase(valeur, fontCellule)));
                }
            }
            document.add(table);
            document.close();

            return new RapportGenere(nomFichier, out.toByteArray(), MediaType.APPLICATION_PDF);
        } catch (DocumentException e) {
            throw new UncheckedIOException(new IOException("Échec de la génération du PDF.", e));
        }
    }

    private RapportGenere renderExcel(String titre, List<String> entetes, List<List<String>> lignes, String nomFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(titre.length() > 31 ? titre.substring(0, 31) : titre);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < entetes.size(); i++) {
                headerRow.createCell(i).setCellValue(entetes.get(i));
            }
            for (int i = 0; i < lignes.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> ligne = lignes.get(i);
                for (int j = 0; j < ligne.size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(ligne.get(j));
                }
            }
            for (int i = 0; i < entetes.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new RapportGenere(nomFichier, out.toByteArray(),
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la génération de l'Excel.", e);
        }
    }
}
