package com.sirh.model.dto;

import com.sirh.model.enums.EtatMouvement;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(@NotNull EtatMouvement etat, String commentaire) {
}
