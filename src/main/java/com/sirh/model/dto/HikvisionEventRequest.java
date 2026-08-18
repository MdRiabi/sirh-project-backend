package com.sirh.model.dto;

import com.sirh.model.enums.MethodeValidation;
import com.sirh.model.enums.TypeEvenement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Payload simplifié du webhook Hikvision (mode Push, cahier-technique.md §1.1). Le protocole ISAPI
 * réel (XML, "AccessControllerEvent") n'a pas été vérifié contre du matériel physique — ce contrat
 * JSON est celui exposé par notre backend ; une éventuelle passerelle de traduction ISAPI → JSON
 * reste à valider lors de l'intégration avec les pointeuses réelles.
 */
public record HikvisionEventRequest(
    @NotBlank String badgeId,
    @NotNull LocalDateTime timestamp,
    @NotNull TypeEvenement typeEvenement,
    @NotNull MethodeValidation methodeValidation) {}
