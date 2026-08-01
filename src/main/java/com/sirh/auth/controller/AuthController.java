package com.sirh.auth.controller;

import com.sirh.auth.dto.ChangePasswordRequest;
import com.sirh.auth.dto.LoginRequest;
import com.sirh.auth.dto.RefreshTokenRequest;
import com.sirh.auth.dto.TokenResponse;
import com.sirh.auth.dto.UserSummary;
import com.sirh.auth.security.SirhUserDetails;
import com.sirh.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                        @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = extractBearerToken(httpRequest);
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(authService.toUserSummary(principal));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal SirhUserDetails principal,
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
