package com.sirh.controller;

import com.sirh.model.dto.HikvisionEventRequest;
import com.sirh.service.PointageBrutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dédié aux pointeuses physiques (mode Push, cahier-technique.md §1.1) — pas de JWT,
 * sécurisé en amont par HikvisionIpAllowlistFilter.
 */
@Tag(
    name = "Hikvision",
    description =
        "Ingestion des pointages bruts par les pointeuses physiques (cahier-technique.md §1.1) —"
            + " pas de JWT, sécurisé par liste blanche IP")
@RestController
@RequestMapping("/api/v1/hikvision")
@RequiredArgsConstructor
public class HikvisionController {

  private final PointageBrutService pointageBrutService;

  @Operation(
      summary = "Recevoir un événement de pointage",
      description =
          "Public (pas de JWT) — l'IP source doit correspondre à une ligne active de `pointeuses`"
              + " (403 sinon, `HikvisionIpAllowlistFilter`). Mode Push des pointeuses Hikvision.")
  @SecurityRequirements
  @PostMapping("/events")
  public ResponseEntity<Void> recevoirEvenement(
      @Valid @RequestBody HikvisionEventRequest request, HttpServletRequest httpRequest) {
    pointageBrutService.enregistrerDepuisHikvision(httpRequest.getRemoteAddr(), request);
    return ResponseEntity.accepted().build();
  }
}
