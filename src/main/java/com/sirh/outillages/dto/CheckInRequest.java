package com.sirh.outillages.dto;

import com.sirh.outillages.entity.EtatMouvement;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(@NotNull EtatMouvement etat, String commentaire) {
}
