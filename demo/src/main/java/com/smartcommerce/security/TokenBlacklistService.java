package com.smartcommerce.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * DSA Principle: HashMap / Hash Table
 * ─────────────────────────────────────────────────────────────────────────────
    * This service implements a token blacklist using a ConcurrentHashMap, which
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /**
     * The core data structure:
        *   Key: SHA-256 hash of the raw JWT string (fixed-length 64-character hex)
     */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a JWT to the blacklist.
     * @param rawToken   the raw JWT string extracted from the Authorization header
     * @param expiration the token's expiration date (obtained from its claims)
     */
    public void revokeToken(String rawToken, Date expiration) {
        String tokenHash = hashToken(rawToken);
        blacklist.put(tokenHash, expiration.getTime());  // O(1) insertion
        log.info("Token revoked — blacklist size: {}", blacklist.size());
    }

    /**
     * @param rawToken the raw JWT string to check
     * @return true if the token is blacklisted; false otherwise
     */
    public boolean isRevoked(String rawToken) {
        String tokenHash = hashToken(rawToken);
        return blacklist.containsKey(tokenHash);  // O(1) lookup
    }

    /**
     * Explicitly removes a token from the blacklist.
     * Useful if the entry was pre-emptively inserted and then needs to be rolled back.
     *
     * @param rawToken the raw JWT string to remove
     */
    public void removeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        blacklist.remove(tokenHash);              // O(1) removal
    }

    /**
     * Returns the current number of entries in the blacklist.
     * Useful for metrics / monitoring.
     */
    public int size() {
        return blacklist.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduled maintenance — prevents unbounded memory growth
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs every 30 minutes and evicts entries whose tokens have already expired.
     * fixedRate = 1_800_000 ms = 30 minutes
     */
    @Scheduled(fixedRate = 1_800_000)
    public void evictExpiredTokens() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);

        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("Blacklist cleanup — removed {} expired entries, {} remaining",
                    removed, blacklist.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param rawToken the raw JWT string
     * @return 64-character lowercase hex string of the SHA-256 digest
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java SE spec — this branch is unreachable
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
