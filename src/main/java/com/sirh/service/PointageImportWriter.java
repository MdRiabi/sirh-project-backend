package com.sirh.service;

import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.TechnicalException;
import com.sirh.model.entity.Employe;
import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.enums.SourceImport;
import com.sirh.model.enums.TypeEvenement;
import com.sirh.model.repository.EmployeRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écriture transactionnelle du fichier d'import (méthode séparée de ImportPointagesService pour que
 * le @Transactional s'applique réellement : appelée depuis une méthode @Async d'un autre bean, donc
 * un appel externe qui passe bien par le proxy Spring — pas un self-invocation).
 */
@Service
@RequiredArgsConstructor
public class PointageImportWriter {

  private static final Logger log = LoggerFactory.getLogger(PointageImportWriter.class);

  private static final String[] COLONNES_ATTENDUES = {"badgeid", "datetime", "typeevenement"};
  private static final DateTimeFormatter TEXTE_DATETIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

  private final EmployeRepository employeRepository;
  private final PointageBrutService pointageBrutService;

  /**
   * Rollback total au 1er échec (cahier-fonctionnel.md Module 1) : toute la méthode est une seule
   * transaction, une ConflictException (IMPORT_*) sur n'importe quelle ligne annule l'ensemble des
   * insertions précédentes.
   */
  @Transactional
  public int importer(byte[] fichier) {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fichier))) {
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> colonnes = lireEntetes(sheet.getRow(sheet.getFirstRowNum()));

      int lignesTraitees = 0;
      for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || estLigneVide(row)) {
          continue;
        }
        int numeroLigne =
            i + 1; // 1-indexé pour les messages d'erreur (aligné sur l'affichage Excel)
        traiterLigne(row, colonnes, numeroLigne);
        lignesTraitees++;
      }
      return lignesTraitees;
    } catch (IOException e) {
      log.error("Fichier Excel illisible.", e);
      throw new TechnicalException(ErrorCode.SYSTEM_ERROR, e);
    }
  }

  private Map<String, Integer> lireEntetes(Row header) {
    if (header == null) {
      throw new ConflictException(ErrorCode.IMPORT_ENTETE_MANQUANTE, new Object[] {1});
    }
    Map<String, Integer> colonnes = new HashMap<>();
    for (Cell cell : header) {
      String nom = valeurTexte(cell).toLowerCase().replace(" ", "").replace("_", "");
      colonnes.put(nom, cell.getColumnIndex());
    }
    for (String attendue : COLONNES_ATTENDUES) {
      if (!colonnes.containsKey(attendue)) {
        throw new ConflictException(ErrorCode.IMPORT_COLONNE_MANQUANTE, new Object[] {1, attendue});
      }
    }
    return colonnes;
  }

  private void traiterLigne(Row row, Map<String, Integer> colonnes, int numeroLigne) {
    String badgeId = valeurTexte(row.getCell(colonnes.get("badgeid")));
    if (badgeId.isBlank()) {
      throw new ConflictException(ErrorCode.IMPORT_BADGE_ID_MANQUANT, new Object[] {numeroLigne});
    }

    Employe employe =
        employeRepository
            .findByMatriculeInterneAndDeletedAtIsNull(badgeId)
            .orElseThrow(
                () ->
                    new ConflictException(
                        ErrorCode.IMPORT_MATRICULE_INEXISTANT,
                        new Object[] {numeroLigne, badgeId}));

    LocalDateTime timestamp = lireDateHeure(row.getCell(colonnes.get("datetime")), numeroLigne);

    TypeEvenement type =
        lireEnum(
            TypeEvenement.class,
            valeurTexte(row.getCell(colonnes.get("typeevenement"))),
            numeroLigne,
            "typeEvenement");

    Integer colMethode = colonnes.get("methodevalidation");
    MethodeValidation methode = MethodeValidation.MANUEL;
    if (colMethode != null) {
      String texte = valeurTexte(row.getCell(colMethode));
      if (!texte.isBlank()) {
        methode = lireEnum(MethodeValidation.class, texte, numeroLigne, "methodeValidation");
      }
    }

    UUID employeId = employe.getId();
    pointageBrutService.enregistrer(
        employeId, null, timestamp, type, methode, SourceImport.EXCEL_IMPORT);
  }

  private <E extends Enum<E>> E lireEnum(
      Class<E> type, String valeur, int numeroLigne, String champ) {
    try {
      return Enum.valueOf(type, valeur.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ConflictException(
          ErrorCode.IMPORT_VALEUR_INVALIDE, new Object[] {numeroLigne, champ, valeur});
    }
  }

  private LocalDateTime lireDateHeure(Cell cell, int numeroLigne) {
    if (cell == null) {
      throw new ConflictException(
          ErrorCode.IMPORT_DATE_HEURE_MANQUANTE, new Object[] {numeroLigne});
    }
    try {
      if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
        return cell.getLocalDateTimeCellValue();
      }
      String texte = valeurTexte(cell).trim();
      return LocalDateTime.parse(texte, TEXTE_DATETIME);
    } catch (Exception e) {
      throw new ConflictException(
          ErrorCode.IMPORT_DATE_HEURE_INVALIDE, new Object[] {numeroLigne, valeurTexte(cell)});
    }
  }

  private boolean estLigneVide(Row row) {
    for (Cell cell : row) {
      if (!valeurTexte(cell).isBlank()) {
        return false;
      }
    }
    return true;
  }

  private String valeurTexte(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC ->
          DateUtil.isCellDateFormatted(cell)
              ? cell.getLocalDateTimeCellValue().toString()
              : String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
  }
}
