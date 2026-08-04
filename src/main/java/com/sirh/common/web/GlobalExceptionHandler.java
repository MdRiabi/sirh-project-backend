package com.sirh.common.web;

import com.sirh.auth.exception.AccountLockedException;
import com.sirh.auth.exception.InvalidCredentialsException;
import com.sirh.auth.exception.InvalidTokenException;
import com.sirh.auth.exception.UtilisateurNotFoundException;
import com.sirh.assiduite.exception.ClotureRefuseeException;
import com.sirh.assiduite.exception.DemandeDeverrouillageNotFoundException;
import com.sirh.assiduite.exception.DemandeDeverrouillageStatutInvalideException;
import com.sirh.assiduite.exception.MoisDejaClotureException;
import com.sirh.assiduite.exception.MoisNonClotureException;
import com.sirh.async.exception.AsyncJobNotFoundException;
import com.sirh.calendrier.exception.JourFerieNotFoundException;
import com.sirh.demandes.exception.AutorisationNotFoundException;
import com.sirh.demandes.exception.DelaiRegularisationDepasseException;
import com.sirh.demandes.exception.DemandeStatutInvalideException;
import com.sirh.demandes.exception.IndisponibiliteNotFoundException;
import com.sirh.demandes.exception.QuotaAutorisationDepasseException;
import com.sirh.demandes.exception.SoldeInsuffisantException;
import com.sirh.fichiers.exception.FichierNotFoundException;
import com.sirh.personnel.exception.ContratNotFoundException;
import com.sirh.personnel.exception.DepartementNotFoundException;
import com.sirh.personnel.exception.EmployeNotFoundException;
import com.sirh.parametres.exception.ParametreNotFoundException;
import com.sirh.pointages.exception.ImportPointagesException;
import com.sirh.pointages.exception.PointageNotFoundException;
import com.sirh.pointages.exception.PointeuseNotFoundException;
import com.sirh.planning.exception.PlanningConflictException;
import com.sirh.planning.exception.ShiftNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex) {
        return build(HttpStatus.LOCKED, ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FichierNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFichierNotFound(FichierNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DepartementNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDepartementNotFound(DepartementNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmployeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeNotFound(EmployeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ContratNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContratNotFound(ContratNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UtilisateurNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUtilisateurNotFound(UtilisateurNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PointeuseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePointeuseNotFound(PointeuseNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PointageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePointageNotFound(PointageNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AsyncJobNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAsyncJobNotFound(AsyncJobNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ImportPointagesException.class)
    public ResponseEntity<Map<String, Object>> handleImportPointages(ImportPointagesException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ShiftNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleShiftNotFound(ShiftNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PlanningConflictException.class)
    public ResponseEntity<Map<String, Object>> handlePlanningConflict(PlanningConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(JourFerieNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleJourFerieNotFound(JourFerieNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ParametreNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleParametreNotFound(ParametreNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AutorisationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAutorisationNotFound(AutorisationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DelaiRegularisationDepasseException.class)
    public ResponseEntity<Map<String, Object>> handleDelaiRegularisationDepasse(DelaiRegularisationDepasseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(QuotaAutorisationDepasseException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaAutorisationDepasse(QuotaAutorisationDepasseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DemandeStatutInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleDemandeStatutInvalide(DemandeStatutInvalideException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IndisponibiliteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleIndisponibiliteNotFound(IndisponibiliteNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SoldeInsuffisantException.class)
    public ResponseEntity<Map<String, Object>> handleSoldeInsuffisant(SoldeInsuffisantException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MoisDejaClotureException.class)
    public ResponseEntity<Map<String, Object>> handleMoisDejaCloture(MoisDejaClotureException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ClotureRefuseeException.class)
    public ResponseEntity<Map<String, Object>> handleClotureRefusee(ClotureRefuseeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MoisNonClotureException.class)
    public ResponseEntity<Map<String, Object>> handleMoisNonCloture(MoisNonClotureException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DemandeDeverrouillageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDemandeDeverrouillageNotFound(DemandeDeverrouillageNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DemandeDeverrouillageStatutInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleDemandeDeverrouillageStatutInvalide(DemandeDeverrouillageStatutInvalideException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Valeur en conflit avec une contrainte d'unicité existante (matricule, email, nom, date ou identifiant déjà utilisé).");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
