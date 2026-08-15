package com.sirh.exception;

import com.sirh.exception.AccountLockedException;
import com.sirh.exception.InvalidCredentialsException;
import com.sirh.exception.InvalidTokenException;
import com.sirh.exception.UtilisateurNotFoundException;
import com.sirh.exception.ClotureRefuseeException;
import com.sirh.exception.DemandeDeverrouillageNotFoundException;
import com.sirh.exception.DemandeDeverrouillageStatutInvalideException;
import com.sirh.exception.MoisDejaClotureException;
import com.sirh.exception.MoisNonClotureException;
import com.sirh.exception.AsyncJobNotFoundException;
import com.sirh.exception.JourFerieNotFoundException;
import com.sirh.exception.AutorisationNotFoundException;
import com.sirh.exception.DelaiRegularisationDepasseException;
import com.sirh.exception.DemandeStatutInvalideException;
import com.sirh.exception.IndisponibiliteNotFoundException;
import com.sirh.exception.QuotaAutorisationDepasseException;
import com.sirh.exception.SoldeInsuffisantException;
import com.sirh.exception.EvaluationIncompleteException;
import com.sirh.exception.EvaluationNotFoundException;
import com.sirh.exception.EvaluationStatutInvalideException;
import com.sirh.exception.FichierNotFoundException;
import com.sirh.exception.NotificationNotFoundException;
import com.sirh.exception.OutillageMouvementInvalideException;
import com.sirh.exception.OutillageNotFoundException;
import com.sirh.exception.ContratNotFoundException;
import com.sirh.exception.DepartementNotFoundException;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.exception.ParametreNotFoundException;
import com.sirh.exception.ImportPointagesException;
import com.sirh.exception.PointageNotFoundException;
import com.sirh.exception.PointeuseNotFoundException;
import com.sirh.exception.PlanningConflictException;
import com.sirh.exception.ShiftNotFoundException;
import com.sirh.exception.TacheRenouvellementNotFoundException;
import com.sirh.exception.TacheRenouvellementStatutInvalideException;
import com.sirh.exception.ReportNotReadyException;
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

    @ExceptionHandler(ReportNotReadyException.class)
    public ResponseEntity<Map<String, Object>> handleReportNotReady(ReportNotReadyException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
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

    @ExceptionHandler(OutillageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOutillageNotFound(OutillageNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OutillageMouvementInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleOutillageMouvementInvalide(OutillageMouvementInvalideException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TacheRenouvellementNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTacheRenouvellementNotFound(TacheRenouvellementNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TacheRenouvellementStatutInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleTacheRenouvellementStatutInvalide(TacheRenouvellementStatutInvalideException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EvaluationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEvaluationNotFound(EvaluationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EvaluationStatutInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleEvaluationStatutInvalide(EvaluationStatutInvalideException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EvaluationIncompleteException.class)
    public ResponseEntity<Map<String, Object>> handleEvaluationIncomplete(EvaluationIncompleteException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("competencesManquantes", ex.getCompetencesManquantes());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
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
