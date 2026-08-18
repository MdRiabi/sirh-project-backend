package com.sirh.controller;

import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ChangePasswordRequest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.RefreshTokenRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.dto.UserSummary;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Auth & Utilisateurs",
    description = "Authentification JWT et profil du compte connecté (cahier-technique.md §4.1)")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(
      summary = "Se connecter",
      description =
          "Public — aucun JWT requis. Renvoie un access token (15 min) et un refresh token (7"
              + " jours).")
  @SecurityRequirements
  @PostMapping("/login")
  public ResponseEntity<SingleObjectDto<TokenResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(SingleObjectDto.of(authService.login(request)));
  }

  @Operation(
      summary = "Rafraîchir l'access token",
      description =
          "Public — aucun JWT requis, échange un refresh token valide contre un nouveau couple de"
              + " tokens.")
  @SecurityRequirements
  @PostMapping("/refresh-token")
  public ResponseEntity<SingleObjectDto<TokenResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(SingleObjectDto.of(authService.refresh(request.refreshToken())));
  }

  @Operation(
      summary = "Se déconnecter",
      description =
          "Invalide l'access token courant et, si fourni, le refresh token (blacklist Redis).")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest httpRequest, @RequestBody(required = false) RefreshTokenRequest request) {
    String accessToken = extractBearerToken(httpRequest);
    String refreshToken = request != null ? request.refreshToken() : null;
    authService.logout(accessToken, refreshToken);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Profil du compte connecté",
      description = "Tout rôle authentifié — résout le profil depuis le JWT courant.")
  @GetMapping("/me")
  public ResponseEntity<SingleObjectDto<UserSummary>> me(
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(SingleObjectDto.of(authService.toUserSummary(principal)));
  }

  @Operation(
      summary = "Changer son mot de passe",
      description = "Tout rôle authentifié — nécessite le mot de passe actuel.")
  @PutMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal SirhUserDetails principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(principal.getUserId(), request);
    return ResponseEntity.noContent().build();
  }

  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
