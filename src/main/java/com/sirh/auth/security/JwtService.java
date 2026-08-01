package com.sirh.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Émission/validation des JWT access + refresh (cahier-technique.md §2,
 * ARCHITECTURE.md §5). Les secrets configurés (JWT_SECRET/JWT_REFRESH_SECRET)
 * peuvent être de longueur arbitraire : ils sont dérivés en clé HMAC-SHA256
 * de taille fixe via SHA-256 pour toujours satisfaire l'exigence de longueur
 * minimale de jjwt, quelle que soit la longueur du secret fourni.
 */
@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        this.accessKey = deriveKey(secret);
        this.refreshKey = deriveKey(refreshSecret);
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    public String generateAccessToken(Long userId, UUID employeId, String username, String role, Long departementId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .claim("employeId", employeId != null ? employeId.toString() : null)
                .claim("role", role)
                .claim("departementId", departementId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirationMinutes * 60)))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenExpirationDays * 24 * 3600)))
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationDays * 24 * 3600;
    }
}