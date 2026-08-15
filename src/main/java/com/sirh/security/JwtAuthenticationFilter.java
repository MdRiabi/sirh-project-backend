package com.sirh.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valide le JWT access token sur chaque requête, vérifie la blacklist Redis
 * (logout) et recharge l'utilisateur en base pour refléter en temps réel un
 * compte désactivé/verrouillé après l'émission du token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public static final String JTI_REQUEST_ATTRIBUTE = "jwt.jti";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                    TokenBlacklistService blacklistService,
                                    CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseAccessToken(token);
            String jti = claims.getId();
            if (!blacklistService.isBlacklisted(jti)) {
                String username = claims.getSubject();
                SirhUserDetails userDetails = (SirhUserDetails) userDetailsService.loadUserByUsername(username);
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute(JTI_REQUEST_ATTRIBUTE, jti);
                }
            }
        } catch (Exception e) {
            log.debug("Token invalide/expiré ou utilisateur introuvable : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}