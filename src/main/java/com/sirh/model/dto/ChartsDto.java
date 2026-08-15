package com.sirh.model.dto;

import java.util.List;

// GET /dashboard/charts (Lot 11) — 🆕 non listé au §4.8 de cahier-technique.md
// (seul /dashboard/kpi y figure), ajouté pour les visualisations du Tableau de
// Bord (CDF Module 10) et documenté a posteriori, même principe que les gaps
// comblés lot par lot (ex. GET /plannings/delta au Lot 3).
public record ChartsDto(
        List<PointDto> evolutionEffectifs,
        List<PointDto> turnoverMensuel,
        List<RepartitionDto> congesEnCours,
        List<PointDto> anomaliesPointageHebdo
) {
}
