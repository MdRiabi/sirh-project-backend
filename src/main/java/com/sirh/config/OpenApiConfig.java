package com.sirh.config;

import java.time.LocalTime;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * Sans ce mapping, springdoc décrit java.time.LocalTime par réflexion sur ses champs internes
 * ({hour, minute, second, nano}) au lieu de la chaîne "HH:mm:ss" réellement produite par Jackson à
 * l'exécution (JavaTimeModule, déjà correct pour LocalDate — seul LocalTime n'a pas de résolveur
 * dédié dans cette version de springdoc) — cassait la génération de types TypeScript côté frontend
 * (openapi-typescript) pour Shift.heureDebut/Fin.
 */
@Configuration
public class OpenApiConfig {
  static {
    SpringDocUtils.getConfig().replaceWithClass(LocalTime.class, String.class);
  }
}
