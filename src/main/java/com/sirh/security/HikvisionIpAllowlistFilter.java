package com.sirh.security;

import com.sirh.service.PointeuseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * cahier-technique.md §1.1 : filtre exécuté avant l'authentification (les pointeuses physiques
 * n'ont pas de JWT) — la table pointeuses.adresse_ip est la source de vérité. Toute requête sur
 * /api/v1/hikvision/** dont l'IP source ne correspond à aucune pointeuse enregistrée est rejetée en
 * 403. Les pointeuses appellent le backend directement sur le port exposé (pas de proxy nginx sur
 * ce chemin, cf. ARCHITECTURE.md §1), donc getRemoteAddr() reflète bien l'IP réelle du boîtier.
 */
@Component
public class HikvisionIpAllowlistFilter extends OncePerRequestFilter {

  private static final String HIKVISION_PATH_PATTERN = "/api/v1/hikvision/**";
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final PointeuseService pointeuseService;

  public HikvisionIpAllowlistFilter(PointeuseService pointeuseService) {
    this.pointeuseService = pointeuseService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!PATH_MATCHER.match(HIKVISION_PATH_PATTERN, request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    String remoteAddr = request.getRemoteAddr();
    if (pointeuseService.findByAdresseIp(remoteAddr).isEmpty()) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"IP non enregistrée dans la"
                  + " liste blanche des pointeuses.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
