package com.sirh.model.dto;

import com.sirh.model.dto.AsyncJobDto;

// Résultat de POST /reports/generer (Module 7, CDF) : Pret si le calcul a
// terminé sous 5s (téléchargement direct), Lance si la tâche continue en
// arrière-plan (suivi via GET /reports/tasks/{taskId}).
public sealed interface ReportGenerationResult {
    record Pret(RapportGenere rapport) implements ReportGenerationResult {
    }

    record Lance(AsyncJobDto job) implements ReportGenerationResult {
    }
}
