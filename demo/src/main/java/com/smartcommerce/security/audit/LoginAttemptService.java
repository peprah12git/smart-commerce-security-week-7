package com.smartcommerce.security.audit;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * DSA Principle: HashMap + sliding-window counter for brute-force detection.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Data structures used
 * ─────────────────────
 * 1. {@code ConcurrentHashMap<String, Deque<Instant>>}
 *      Key   = username (or IP)
 *      Value = time-ordered deque of failed-attempt timestamps
 *
 *    A Deque (double-ended queue) allows O(1) append to the tail and O(1)
 *    removal from the head — perfect for a sliding time-window because we
 *    only ever need to drop old entries from the front.
 *
 *    Time complexity per operation:
 *      recordFailure()  — O(k)  where k = entries outside the window (amortised O(1))
 *      isBlocked()      — O(k)  same eviction sweep
 *      reset()          — O(1)  ConcurrentHashMap.remove
 *
 * 2. {@code ConcurrentHashMap<String, Long>} for block-until timestamps
 *      When a user is blocked, we record when the block expires.
 *      isBlocked() is then a pure O(1) Instant comparison.
 *
 * Brute-force prevention mechanism
 * ──────────────────────────────────
 *   After MAX_FAILURES attempts within WINDOW_SECONDS the account is soft-locked
 *   for LOCKOUT_SECONDS.  Every call to recordFailure() slides the window by
 *   removing timestamps older than Instant.now() - WINDOW_SECONDS from the
 *   head of the deque before counting — this is the "sliding window" algorithm.
 *
 *   Example  (MAX_FAILURES=5, WINDOW=60 s, LOCKOUT=300 s):
 *     T=0   → attempt 1
 *     T=10  → attempt 2
 *     T=20  → attempt 3
 *     T=30  → attempt 4
 *     T=40  → attempt 5  ← threshold hit, blocked until T=340
 *     T=41  → isBlocked() returns true — 401 returned immediately, no DB hit
 *     T=341 → block expired, counter reset
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    /** Maximum failed attempts before a soft-lock is applied. */
    @Value("${security.login.max-failures:5}")
    private int maxFailures;

    /** Sliding window length in seconds over which failures are counted. */
    @Value("${security.login.window-seconds:60}")
    private long windowSeconds;

    /** How long (seconds) the account is locked after breaching the threshold. */
    @Value("${security.login.lockout-seconds:300}")
    private long lockoutSeconds;

    // ── Core data structures ─────────────────────────────────────────────────

    /**
     * Sliding-window attempt log.
     * Key = username; Value = deque of attempt timestamps (newest at tail).
     * ConcurrentHashMap: O(1) per-key access; thread-safe without global lock.
     */
    private final ConcurrentHashMap<String, Deque<Instant>> attemptLog =
            new ConcurrentHashMap<>();

    /**
     * Active lockouts.
     * Key = username; Value = Instant when the lockout expires.
     * O(1) lookup on every login attempt.
     */
    private final ConcurrentHashMap<String, Instant> lockoutMap =
            new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records a failed login attempt and applies a lockout if the threshold
     * is breached.  Returns true when a NEW lockout was just triggered (so the
     * caller can emit a BRUTE_FORCE_DETECTED audit event exactly once).
     */
    public boolean recordFailure(String username) {
        Instant now      = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> attempts = attemptLog.computeIfAbsent(username, k -> new LinkedList<>());

        synchronized (attempts) {
            // Slide the window — evict timestamps older than windowStart
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
                attempts.pollFirst();   // O(1) head removal
            }
            attempts.addLast(now);      // O(1) tail append

            if (attempts.size() >= maxFailures && !lockoutMap.containsKey(username)) {
                lockoutMap.put(username, now.plusSeconds(lockoutSeconds));
                log.warn("[AUDIT] BRUTE_FORCE_DETECTED username={} failures={} window={}s lockout={}s",
                        username, attempts.size(), windowSeconds, lockoutSeconds);
                return true;  // new lockout just triggered
            }
        }
        return false;
    }

    /**
     * Returns true if the user is currently under a soft-lock.
     * O(1) — single ConcurrentHashMap lookup + Instant comparison.
     */
    public boolean isBlocked(String username) {
        Instant until = lockoutMap.get(username);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            // Lockout has naturally expired — clean up both maps
            lockoutMap.remove(username);
            attemptLog.remove(username);
            return false;
        }
        return true;
    }

    /**
     * Returns the number of failed attempts in the current window for {@code username}.
     * Used by the audit service for log enrichment.
     */
    public int failureCount(String username) {
        Deque<Instant> attempts = attemptLog.get(username);
        if (attempts == null) return 0;
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);
        synchronized (attempts) {
            return (int) attempts.stream().filter(t -> t.isAfter(windowStart)).count();
        }
    }

    /** Clears the failure record on successful login (reset after good auth). */
    public void resetFailures(String username) {
        attemptLog.remove(username);    // O(1)
        lockoutMap.remove(username);    // O(1)
    }

    /** Returns the total number of users currently being tracked. */
    public int trackedUserCount() {
        return attemptLog.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduled maintenance
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Purges stale entries every 10 minutes to prevent unbounded map growth.
     * O(n) sweep — amortised cost is negligible because it runs infrequently.
     */
    @Scheduled(fixedRate = 600_000)
    public void evictExpiredEntries() {
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);

        attemptLog.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                // Remove head entries older than the window
                while (!entry.getValue().isEmpty()
                        && entry.getValue().peekFirst().isBefore(windowStart)) {
                    entry.getValue().pollFirst();
                }
                return entry.getValue().isEmpty();
            }
        });

        lockoutMap.entrySet().removeIf(entry -> Instant.now().isAfter(entry.getValue()));

        log.debug("[AUDIT] LoginAttemptService cleanup — tracked users: {}", attemptLog.size());
    }
}
