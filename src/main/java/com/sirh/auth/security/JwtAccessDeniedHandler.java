package com.sirh.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Sans AccessDeniedHandler explicite, Spring Security retombe sur le handler
 * par défaut qui délègue à la gestion d'erreur générique de Tomcat/Boot — en
 * pratique celle-ci renvoie 401 (via JwtAuthenticationEntryPoint) au lieu de
 * 403 pour un utilisateur authentifié mais sans le rôle requis. Ce handler
 * écrit directement la réponse, sans redispatch, pour garantir le bon code.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", "Accès refusé : rôle insuffisant pour cette action."
        ));
    }
}