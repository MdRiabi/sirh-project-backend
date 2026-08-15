package com.sirh.service;

import com.sirh.model.dto.ChangePasswordRequest;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.dto.UserSummary;
import com.sirh.model.entity.Utilisateur;
import com.sirh.exception.AccountLockedException;
import com.sirh.exception.InvalidCredentialsException;
import com.sirh.exception.InvalidTokenException;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.security.JwtService;
import com.sirh.security.SirhUserDetails;
import com.sirh.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class AuthService {

    private static final int MAX_ECHECS_CONSECUTIFS = 5;
    private static final long VERROUILLAGE_MINUTES = 15;

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        TokenBlacklistService blacklistService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    // noRollbackFor : registerFailedAttempt() doit persister l'incrémentation du
    // compteur d'échecs même quand la méthode se termine par
    // InvalidCredentialsException -- le rollback par défaut de Spring sur toute
    // RuntimeException annulait sinon systématiquement ce compteur, rendant le
    // verrouillage après 5 échecs totalement inopérant.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByUsernameAndDeletedAtIsNull(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides."));

        LocalDateTime verrouilleJusquA = utilisateur.getVerrouilleJusquA();
        if (verrouilleJusquA != null && verrouilleJusquA.isAfter(LocalDateTime.now())) {
            throw new AccountLockedException(verrouilleJusquA);
        }

        if (!Boolean.TRUE.equals(utilisateur.getCompteActif())
                || !passwordEncoder.matches(request.password(), utilisateur.getPasswordHash())) {
            registerFailedAttempt(utilisateur);
            throw new InvalidCredentialsException("Identifiants invalides.");
        }

        utilisateur.setEchecsLoginConsecutifs(0);
        utilisateur.setVerrouilleJusquA(null);
        utilisateur.setDernierLogin(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        return buildTokenResponse(utilisateur);
    }

    private void registerFailedAttempt(Utilisateur utilisateur) {
        int echecs = (utilisateur.getEchecsLoginConsecutifs() == null ? 0 : utilisateur.getEchecsLoginConsecutifs()) + 1;
        utilisateur.setEchecsLoginConsecutifs(echecs);
        if (echecs >= MAX_ECHECS_CONSECUTIFS) {
            utilisateur.setVerrouilleJusquA(LocalDateTime.now().plusMinutes(VERROUILLAGE_MINUTES));
        }
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Refresh token invalide ou expiré.");
        }

        if (blacklistService.isBlacklisted(claims.getId())) {
            throw new InvalidTokenException("Refresh token révoqué.");
        }

        Utilisateur utilisateur = utilisateurRepository.findByUsernameAndDeletedAtIsNull(claims.getSubject())
                .orElseThrow(() -> new InvalidTokenException("Utilisateur introuvable."));

        if (!Boolean.TRUE.equals(utilisateur.getCompteActif())) {
            throw new InvalidTokenException("Compte désactivé.");
        }

        blacklistRemainingLifetime(claims.getId(), claims.getExpiration());

        return buildTokenResponse(utilisateur);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try {
                Claims claims = jwtService.parseAccessToken(accessToken);
                blacklistRemainingLifetime(claims.getId(), claims.getExpiration());
            } catch (JwtException | IllegalArgumentException ignored) {
                // token déjà invalide/expiré : rien à révoquer
            }
        }
        if (refreshToken != null) {
            try {
                Claims claims = jwtService.parseRefreshToken(refreshToken);
                blacklistRemainingLifetime(claims.getId(), claims.getExpiration());
            } catch (JwtException | IllegalArgumentException ignored) {
                // idem
            }
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur introuvable."));

        if (!passwordEncoder.matches(request.currentPassword(), utilisateur.getPasswordHash())) {
            throw new InvalidCredentialsException("Mot de passe actuel incorrect.");
        }

        utilisateur.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        utilisateurRepository.save(utilisateur);
    }

    public UserSummary toUserSummary(SirhUserDetails principal) {
        return new UserSummary(
                principal.getUserId(),
                principal.getEmployeId(),
                principal.getUsername(),
                principal.getNom(),
                principal.getPrenom(),
                principal.getRole().name(),
                principal.getDepartementId()
        );
    }

    private TokenResponse buildTokenResponse(Utilisateur utilisateur) {
        var employe = utilisateur.getEmploye();
        String accessToken = jwtService.generateAccessToken(
                utilisateur.getId(), employe.getId(), utilisateur.getUsername(),
                utilisateur.getRole().name(), employe.getDepartementId());
        String refreshToken = jwtService.generateRefreshToken(utilisateur.getId(), utilisateur.getUsername());

        UserSummary summary = new UserSummary(
                utilisateur.getId(), employe.getId(), utilisateur.getUsername(),
                employe.getNom(), employe.getPrenom(), utilisateur.getRole().name(), employe.getDepartementId());

        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds(), summary);
    }

    private void blacklistRemainingLifetime(String jti, Date expiration) {
        long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        blacklistService.blacklist(jti, ttlSeconds);
    }
}
