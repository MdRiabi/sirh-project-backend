package com.sirh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sans AccessDeniedHandler explicite, Spring Security retombe sur le handler
 * par défaut qui délègue à la gestion d'erreur générique de Tomcat/Boot — en
 * pratique celle-ci renvoie 401 (via JwtAuthenticationEntryPoint) au lieu de
 * 403 pour un utilisateur authentifié mais sans le rôle requis. Ce handler
 * écrit directement la réponse, sans redispatch, pour garantir le bon code.
 * Hors périmètre de GlobalExceptionHandler (déclenché au niveau du filtre
 * Security, avant DispatcherServlet) mais le corps JSON reste le même contrat
 * ErrorResponse.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        String message = messageSource.getMessage(ErrorCode.ACCES_REFUSE.name(), null, LocaleContextHolder.getLocale());
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(403, ErrorCode.ACCES_REFUSE.name(), message, request.getRequestURI()));
    }
}