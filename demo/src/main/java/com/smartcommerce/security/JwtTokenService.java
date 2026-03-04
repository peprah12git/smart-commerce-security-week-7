package com.smartcommerce.security;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * DSA Principles applied in this service
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. Hashing (HMAC-SHA256 / BCrypt analogy)
 *    JWT signatures use HMAC-SHA256, a keyed hash function.
 *    Like BCrypt for passwords, the hash is deterministic given the same key,
 *    making forgery computationally infeasible (pre-image resistance).
 *    Validation is O(1) — re-sign the header+payload and compare digests.
 *
 * 2. Blacklist integration (O(1) HashMap lookup)
 *    Before accepting any token as valid, we consult the TokenBlacklistService,
 *    which stores revoked tokens in a ConcurrentHashMap.  The additional check
 *    is pure O(1) and adds negligible latency compared to the JWT parse step.
 */
@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    // Injected via constructor — no circular dependency
    // (TokenBlacklistService has no dependency on JwtTokenService)
    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenService(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token generation
    // ─────────────────────────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates a JWT in three sequential O(1) steps:
     *
     *   Step 1 – Signature / parse check: re-computes HMAC-SHA256 over the
     *            header.payload and compares it to the embedded signature.
     *            Any tampering is detected here. O(1) — fixed digest size.
     *
     *   Step 2 – Expiry check: compares the {@code exp} claim to the current
     *            clock. O(1) integer comparison.
     *
     *   Step 3 – Blacklist check: O(1) HashMap.containsKey() lookup on the
     *            SHA-256 hash of the token.  Ensures revoked (logged-out)
     *            tokens are rejected even while they are still signature-valid
     *            and not yet expired.
     *
     * Combined: O(1) total — constant time regardless of user population size.
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
            if (isTokenExpired(token)) {
                return false;
            }

            // Step 3: blacklist check — O(1) HashMap.containsKey()
            if (tokenBlacklistService.isRevoked(token)) {
                return false;
            }

            return true;
        } catch (Exception e) {
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
