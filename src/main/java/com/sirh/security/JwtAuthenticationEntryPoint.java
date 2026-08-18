package com.sirh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  public JwtAuthenticationEntryPoint(ObjectMapper objectMapper, MessageSource messageSource) {
    this.objectMapper = objectMapper;
    this.messageSource = messageSource;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    String message =
        messageSource.getMessage(
            ErrorCode.NON_AUTHENTIFIE.name(), null, LocaleContextHolder.getLocale());
    objectMapper.writeValue(
        response.getWriter(),
        ErrorResponse.of(401, ErrorCode.NON_AUTHENTIFIE.name(), message, request.getRequestURI()));
  }
}
