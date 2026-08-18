package com.sirh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Vérification interne de disponibilité du service")
@RestController
public class HealthController {

  @Operation(summary = "Vérifier la disponibilité du service")
  @GetMapping("/health")
  public String health() {
    return "ok";
  }
}
