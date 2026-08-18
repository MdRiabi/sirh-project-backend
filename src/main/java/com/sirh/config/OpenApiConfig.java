package com.sirh.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

/**
 * Sans ce mapping, springdoc décrit java.time.LocalTime par réflexion sur ses champs internes
 * ({hour, minute, second, nano}) au lieu de la chaîne "HH:mm:ss" réellement produite par Jackson à
 * l'exécution (JavaTimeModule, déjà correct pour LocalDate — seul LocalTime n'a pas de résolveur
 * dédié dans cette version de springdoc) — cassait la génération de types TypeScript côté frontend
 * (openapi-typescript) pour Shift.heureDebut/Fin.
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {
  static {
    SpringDocUtils.getConfig().replaceWithClass(LocalTime.class, String.class);
  }

  private static final Pattern ROLE_PATTERN = Pattern.compile("ROLE_\\w+");

  @Bean
  public OpenAPI sirhOpenApi() {
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .info(
            new Info()
                .title("SIRH — API REST")
                .version("v1")
                .description(
                    """
                    API du Système d'Information RH (SIRH).

                    **Authentification** : JWT Bearer (`Authorization: Bearer <token>`), obtenu via \
                    `POST /auth/login`. Endpoints publics (aucun JWT requis) : `/auth/login`, \
                    `/auth/refresh-token`, `/api/v1/hikvision/**` (pointeuses physiques, \
                    sécurisées par liste blanche IP à la place).

                    **Rôles** : `EMPLOYE` (lecture seule de son propre planning uniquement), \
                    `MANAGER` (équipe de son département), `RH` (gestion complète, hors logs), \
                    `DIRECTEUR` (lecture seule dashboard), `SUPERUSER` (droits RH + accès exclusif \
                    aux logs d'audit). Les rôles autorisés par endpoint sont listés dans la \
                    description de chaque opération ci-dessous.

                    **Enveloppes de réponse** : les listes sont renvoyées sous \
                    `{ "content": [...], "page", "size", "totalElements", "totalPages" }` \
                    (`ListingPageDto`), les objets uniques sous `{ "data": {...} }` \
                    (`SingleObjectDto`). Les corps de réponse (`*Dto`) sont sérialisés en \
                    `snake_case`, les corps de requête (`*Request`) restent en `camelCase`.

                    **Identifiants** : `employes.id` est un UUID (string), jamais un entier.
                    """));
  }

  /**
   * Dérive automatiquement la doc de sécurité (rôles autorisés, réponses 401/403) depuis
   * l'annotation @PreAuthorize déjà posée sur chaque handler, plutôt que de dupliquer cette
   * information à la main sur une centaine d'endpoints.
   */
  @Bean
  public OperationCustomizer securityDocCustomizer() {
    return (Operation operation, HandlerMethod handlerMethod) -> {
      boolean isPublic = handlerMethod.hasMethodAnnotation(SecurityRequirements.class);
      if (isPublic) {
        return operation;
      }

      PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
      if (preAuthorize == null) {
        // Certains contrôleurs posent @PreAuthorize au niveau classe plutôt que méthode.
        preAuthorize = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
      }
      if (preAuthorize != null) {
        Set<String> roles = new LinkedHashSet<>();
        Matcher matcher = ROLE_PATTERN.matcher(preAuthorize.value());
        while (matcher.find()) {
          roles.add(matcher.group().replace("ROLE_", ""));
        }
        if (!roles.isEmpty()) {
          String rolesLine = "**Rôles autorisés : " + String.join(", ", roles) + "**";
          String existing = operation.getDescription();
          operation.setDescription(existing == null ? rolesLine : rolesLine + "\n\n" + existing);
        }
        operation
            .getResponses()
            .addApiResponse(
                "403",
                new ApiResponse()
                    .description("Rôle insuffisant pour cette opération")
                    .content(new Content()));
      }

      operation
          .getResponses()
          .addApiResponse(
              "401",
              new ApiResponse()
                  .description("JWT absent, invalide ou expiré")
                  .content(new Content()));

      return operation;
    };
  }
}
