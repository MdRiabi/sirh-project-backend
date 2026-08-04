package com.sirh.assiduite.dto;

import jakarta.validation.constraints.Pattern;

// statut : APPROUVEE ou REFUSEE (seules valeurs acceptées par
// CloturePaieService.traiterDemande en dehors de EN_ATTENTE, son statut initial).
public record DemandeDeverrouillageTraiterRequest(@Pattern(regexp = "APPROUVEE|REFUSEE") String statut) {
}
