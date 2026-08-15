package com.sirh.security;

import com.sirh.security.HikvisionIpAllowlistFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/refresh-token",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Pointeuses physiques : pas de JWT, sécurisé par liste blanche IP
            // (HikvisionIpAllowlistFilter, cf. cahier-technique.md §1.1).
            "/api/v1/hikvision/**",
            // Forward interne Spring Boot suite à response.sendError() (ex. 400
            // MethodArgumentNotValidException) : JwtAuthenticationFilter ne
            // rejoue pas l'authentification sur un dispatch ERROR
            // (OncePerRequestFilter.shouldNotFilterErrorDispatch() = true par
            // défaut), donc sans cette entrée le forward vers /error atterrissait
            // non authentifié et l'entry point écrasait la vraie erreur (400, 409...)
            // par un 401 générique trompeur — bug pré-existant (Lot 0), trouvé en
            // validant manuellement le Lot 7.
            "/error"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final HikvisionIpAllowlistFilter hikvisionIpAllowlistFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           JwtAuthenticationEntryPoint authenticationEntryPoint,
                           JwtAccessDeniedHandler accessDeniedHandler,
                           HikvisionIpAllowlistFilter hikvisionIpAllowlistFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.hikvisionIpAllowlistFilter = hikvisionIpAllowlistFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(hikvisionIpAllowlistFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
