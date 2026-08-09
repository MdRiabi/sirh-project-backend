package com.sirh.reporting.dto;

import java.math.BigDecimal;

// Point générique d'une série temporelle (courbe/histogramme) — mois (YYYY-MM)
// ou semaine (YYYY-MM-DD du lundi) selon le graphique, cf. ChartsDto.
public record PointDto(String label, BigDecimal valeur) {
}
