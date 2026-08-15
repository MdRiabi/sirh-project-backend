package com.sirh.exception;

import java.util.UUID;

// GET /reports/tasks/{reference}/download appelé avant la fin du calcul, ou sur un
// job en échec — 409, le job existe bien (sinon AsyncJobNotFoundException).
public class ReportNotReadyException extends RuntimeException {
    public ReportNotReadyException(UUID reference) {
        super("Le rapport " + reference + " n'est pas encore prêt au téléchargement.");
    }
}
