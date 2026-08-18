package com.sirh.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirh.annotation.Audited;
import com.sirh.model.entity.AuditLog;
import com.sirh.model.repository.AuditLogRepository;
import com.sirh.security.SirhUserDetails;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public AuditAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  // Volontairement @AfterReturning (pas @Around) : l'action métier n'est jamais
  // bloquée par un incident du pipeline d'audit ; un échec d'écriture est
  // seulement journalisé côté application. À revisiter si une garantie
  // d'atomicité audit+action devient une exigence de conformité explicite.
  @AfterReturning("@annotation(audited)")
  public void logAfterSuccess(JoinPoint joinPoint, Audited audited) {
    try {
      AuditLog entry =
          AuditLog.builder()
              .utilisateurId(currentUtilisateurId())
              .action(audited.action())
              .tableCible(audited.tableCible())
              .nouvellesValeurs(safeWriteValue(joinPoint.getArgs()))
              .adresseIp(currentIp())
              .build();
      auditLogRepository.save(entry);
    } catch (Exception e) {
      log.error("Échec de l'écriture du log d'audit pour l'action {}", audited.action(), e);
    }
  }

  private Long currentUtilisateurId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof SirhUserDetails userDetails) {
      return userDetails.getUserId();
    }
    return null;
  }

  private String currentIp() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      return attributes.getRequest().getRemoteAddr();
    }
    return null;
  }

  private String safeWriteValue(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return null;
    }
  }
}
