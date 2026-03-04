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
 * A JWT blacklist implemented as an in-memory ConcurrentHashMap.
 *
 * Key   → SHA-256 digest of the raw JWT string   (32-byte hex, fixed length)
 * Value → expiration epoch-millis                (used for scheduled cleanup)
 *
 * Time Complexity
 *   • revokeToken   – O(1) average  (HashMap put)
 *   • isRevoked     – O(1) average  (HashMap containsKey)
 *   • removeToken   – O(1) average  (HashMap remove)
 *   • scheduledCleanup – O(n) once per interval, keeps map bounded
 *
 * Why ConcurrentHashMap instead of HashMap?
 *   Multiple request threads can revoke / check tokens simultaneously.
 *   ConcurrentHashMap provides segment-level locking, giving us thread-safe
 *   O(1) operations without a global lock bottleneck.
 *
 * Why store the hash of the token, not the raw token?
 *   • Reduces memory footprint (256 bits vs. ~500+ bytes per JWT).
 *   • Eliminates risk of exposing a valid token from the in-process heap
 *     (e.g. via heap-dump or memory-inspection attacks).
 *   • Collision risk with SHA-256 is cryptographically negligible.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /**
     * The core data structure:
     *   Key   = SHA-256(rawToken) – O(1) hashed lookup key
     *   Value = token expiration timestamp in epoch-milliseconds
     *
     * ConcurrentHashMap is chosen over HashMap for thread-safety without
     * coarse synchronization, and over Collections.synchronizedMap because
     * it allows concurrent reads with minimal contention.
     */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a JWT to the blacklist.
     * Stores the SHA-256 hash of the token for memory efficiency and security.
     *
     * @param rawToken   the raw JWT string extracted from the Authorization header
     * @param expiration the token's expiration date (obtained from its claims)
     */
    public void revokeToken(String rawToken, Date expiration) {
        String tokenHash = hashToken(rawToken);
        blacklist.put(tokenHash, expiration.getTime());  // O(1) insertion
        log.info("Token revoked — blacklist size: {}", blacklist.size());
    }

    /**
     * Checks whether a JWT has been revoked.
     * O(1) average-case lookup — equivalent performance to reading from L2 cache
     * compared to a database query (O(log n) B-tree or network round-trip).
     *
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
     *
     * DSA Note: O(n) sweep over the map entries. Because this is only called
     * periodically (not on every request), the amortised cost per token-check
     * remains O(1). This is the same strategy used by Redis TTL eviction.
     *
     * fixedRate = 1_800_000 ms = 30 minutes
     */
    @Scheduled(fixedRate = 1_800_000)
    public void evictExpiredTokens() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();

        /*
         * ConcurrentHashMap.entrySet().removeIf() is safe to call
         * without external locking — it iterates the live view and
         * removes matching entries atomically per segment.
         */
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
     * Produces a deterministic, fixed-length SHA-256 hex digest of the token.
     *
     * Hashing principle:
     *   The same JWT always produces the same digest (deterministic), so we can
     *   use it as a HashMap key. SHA-256 distributes keys uniformly across the
     *   256-bit space, minimising hash collisions and ensuring O(1) bucket access.
     *
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
