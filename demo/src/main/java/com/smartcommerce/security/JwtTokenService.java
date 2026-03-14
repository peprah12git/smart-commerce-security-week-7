package com.smartcommerce.security;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartcommerce.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;


@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    // (TokenBlacklistService has no dependency on JwtTokenService)
    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenService(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    void validateJwtConfiguration() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Missing required property: jwt.secret (Base64-encoded HMAC key)");
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("Invalid jwt.secret: decoded key must be at least 32 bytes for HS256");
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid jwt.secret: must be valid Base64 content", ex);
        }

        if (jwtExpiration <= 0) {
            throw new IllegalStateException("Invalid jwt.expiration: value must be greater than 0 milliseconds");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token generation
    // ─────────────────────────────────────────────────────────────────────────

    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required for token generation");
        }

        String role = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
        List<String> roles = List.of("ROLE_" + role);

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     *
     * @param token the raw JWT string from the Authorization header
     * @return true only when the token passes all three checks
     */
    public boolean validateToken(String token) {
        try {
            // Step 1: parse + HMAC-SHA256 signature verification
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            // Step 2: expiry check
            // Step 3: blacklist check — O(1) HashMap.containsKey()
            return !isTokenExpired(token) && !tokenBlacklistService.isRevoked(token);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revocation (logout support)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convenience method — extracts the token's own expiry claim and delegates
     * to the blacklist service.  Called by the logout endpoint so that the
     * token is automatically evicted from the HashMap once it would have
     * expired anyway (no memory leak).
     *
     * @param rawToken the raw JWT string to revoke
     */
    public void revokeToken(String rawToken) {
        Date expiration = extractClaim(rawToken, Claims::getExpiration);
        tokenBlacklistService.revokeToken(rawToken, expiration);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Claims extraction
    // ─────────────────────────────────────────────────────────────────────────

    public String getEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
