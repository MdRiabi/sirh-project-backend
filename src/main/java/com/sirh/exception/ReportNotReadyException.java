package com.sirh.exception;

// GET /reports/tasks/{id}/download appelé avant la fin du calcul, ou sur un
// job en échec — 409, le job existe bien (sinon AsyncJobNotFoundException).
public class ReportNotReadyException extends RuntimeException {
    public ReportNotReadyException(Long taskId) {
        super("Le rapport " + taskId + " n'est pas encore prêt au téléchargement.");
    }
}
