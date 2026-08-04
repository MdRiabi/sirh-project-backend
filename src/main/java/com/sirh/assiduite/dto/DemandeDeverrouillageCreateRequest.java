package com.sirh.assiduite.dto;

import jakarta.validation.constraints.NotBlank;

// mois en String, pas YearMonth — cf. ClotureRequest pour la raison (écart
// schéma OpenAPI / wire format Jackson réel sur java.time.YearMonth).
public record DemandeDeverrouillageCreateRequest(@NotBlank String mois, @NotBlank String motif) {
}
