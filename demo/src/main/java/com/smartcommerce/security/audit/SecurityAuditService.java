package com.smartcommerce.security.audit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Central audit logger for all security events.
 *
 * ─── Responsibilities ─────────────────────────────────────────────────────
 *  1. Log every SecurityAuditEvent as a structured, JSON-friendly SLF4J
 *     message consumable by Logback / Log4j2 with a JSON encoder appender.
 *
 *  2. Track token-usage and endpoint-access frequency in ConcurrentHashMaps
 *     for in-process anomaly detection (O(1) per increment).
 *
 *  3. Detect high-frequency requests from a single IP and emit a
 *     HIGH_FREQUENCY_REQUEST event when the threshold is exceeded.
 *
 * ─── DSA Principles ──────────────────────────────────────────────────────
 *  • ConcurrentHashMap<String, AtomicLong>
 *      Key   = endpoint path (or IP)
 *      Value = AtomicLong hit counter
 *    AtomicLong.incrementAndGet() is a single CAS instruction — O(1) and
 *    lock-free even under high concurrency.
 *
 *  • The frequency maps are bounded by a @Scheduled reset every minute,
 *    so they function as a sliding-tumbling window counter without
 *    unbounded memory growth.
 *
 * ─── Log format ──────────────────────────────────────────────────────────
 * All log statements use the key=value pattern, making them trivially
 * parseable by any log aggregator without a custom Grok pattern:
 *
 *   eventType=LOGIN_FAILURE username=alice@example.com ipAddress=10.0.0.1
 *   endpoint=/api/auth/login httpMethod=POST
 *   timestamp=2026-03-04T14:22:07.341Z details="Bad credentials"
 *
 * When a Logback JSON encoder (logstash-logback-encoder) is on the
 * classpath, each MDC key becomes a top-level JSON field automatically.
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final LoginAttemptService loginAttemptService;

    /** Maximum requests per IP per minute before a HIGH_FREQUENCY event fires. */
    @Value("${security.audit.rate-limit-per-minute:100}")
    private long rateLimitPerMinute;

    // ── Frequency tracking maps ───────────────────────────────────────────────

    /**
     * Counts requests per endpoint path within the current monitoring window.
     * Key = "METHOD /path";  Value = hit count.
     * O(1) increment via AtomicLong CAS.
     */
    private final ConcurrentHashMap<String, AtomicLong> endpointFrequency =
            new ConcurrentHashMap<>();

    /**
     * Counts requests per source IP within the current monitoring window.
     * Used to detect high-rate scraping / DoS patterns.
     */
    private final ConcurrentHashMap<String, AtomicLong> ipFrequency =
            new ConcurrentHashMap<>();

    public SecurityAuditService(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core logging API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records and logs a security event.
     *
     * @param event the fully-constructed audit event
     */
    public void record(SecurityAuditEvent event) {
        // Structured log — one line per event, parseable without regex
        String logLine = buildLogLine(event);

        switch (event.getEventType()) {
            case LOGIN_SUCCESS, LOGOUT ->
                    log.info(logLine);

            case LOGIN_FAILURE, JWT_VALIDATION_FAILURE, REVOKED_TOKEN_REUSE ->
                    log.warn(logLine);

            case ACCESS_DENIED ->
                    log.warn(logLine);

            case BRUTE_FORCE_DETECTED, HIGH_FREQUENCY_REQUEST ->
                    log.error(logLine);

            default ->
                    log.info(logLine);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Frequency tracking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Increments hit counters for both the endpoint and the source IP.
     * Returns true if the IP rate limit was breached (so the caller can fire
     * a HIGH_FREQUENCY_REQUEST event exactly once per breach).
     *
     * O(1) — two ConcurrentHashMap lookups + AtomicLong CAS each.
     */
    public boolean trackRequest(HttpServletRequest request) {
        String key    = request.getMethod() + " " + request.getRequestURI();
        String ip     = resolveClientIp(request);

        endpointFrequency.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        long ipCount = ipFrequency.computeIfAbsent(ip, k -> new AtomicLong(0)).incrementAndGet();

        return ipCount >= rateLimitPerMinute;
    }

    /**
     * Returns the current hit count for a specific endpoint key.
     * Useful for admin dashboards or metrics endpoints.
     */
    public long endpointHits(String methodAndPath) {
        AtomicLong counter = endpointFrequency.get(methodAndPath);
        return counter != null ? counter.get() : 0L;
    }

    /**
     * Returns the current request count for a source IP in the active window.
     */
    public long ipHits(String ip) {
        AtomicLong counter = ipFrequency.get(ip);
        return counter != null ? counter.get() : 0L;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience factory methods (reduce boilerplate at call sites)
    // ─────────────────────────────────────────────────────────────────────────

    public void loginSuccess(String username, HttpServletRequest request) {
        loginAttemptService.resetFailures(username);
        record(SecurityAuditEvent.builder(SecurityEventType.LOGIN_SUCCESS)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details("Authentication successful")
                .build());
    }

    public void loginFailure(String username, HttpServletRequest request, String reason) {
        boolean newLockout = loginAttemptService.recordFailure(username);
        int failureCount   = loginAttemptService.failureCount(username);

        record(SecurityAuditEvent.builder(SecurityEventType.LOGIN_FAILURE)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details(String.format("%s | failures_in_window=%d", reason, failureCount))
                .build());

        if (newLockout) {
            record(SecurityAuditEvent.builder(SecurityEventType.BRUTE_FORCE_DETECTED)
                    .username(username)
                    .ipAddress(resolveClientIp(request))
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .details(String.format(
                            "Soft-lock applied after %d failures — possible brute-force attack",
                            failureCount))
                    .build());
        }
    }

    public void logout(String username, HttpServletRequest request) {
        record(SecurityAuditEvent.builder(SecurityEventType.LOGOUT)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details("JWT revoked and added to blacklist")
                .build());
    }

    public void jwtValidationFailure(HttpServletRequest request, String reason) {
        record(SecurityAuditEvent.builder(SecurityEventType.JWT_VALIDATION_FAILURE)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details(reason)
                .build());
    }

    public void revokedTokenReuse(String username, HttpServletRequest request) {
        record(SecurityAuditEvent.builder(SecurityEventType.REVOKED_TOKEN_REUSE)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details("Presentation of a previously revoked (blacklisted) token detected")
                .build());
    }

    public void accessDenied(String username, HttpServletRequest request) {
        record(SecurityAuditEvent.builder(SecurityEventType.ACCESS_DENIED)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details("Insufficient role/authority for requested resource")
                .build());
    }

    public void highFrequencyRequest(String ip, HttpServletRequest request, long count) {
        record(SecurityAuditEvent.builder(SecurityEventType.HIGH_FREQUENCY_REQUEST)
                .ipAddress(ip)
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .details(String.format(
                        "IP exceeded rate limit — %d requests in current window", count))
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduled window reset
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resets the frequency counters every 60 seconds.
     * This creates a tumbling (non-overlapping) 1-minute window.
     * Endpoints + IPs that generated activity in the last window are
     * logged as an access-frequency snapshot before the reset.
     */
    @Scheduled(fixedRate = 60_000)
    public void resetFrequencyCounters() {
        if (!endpointFrequency.isEmpty()) {
            endpointFrequency.forEach((endpoint, counter) ->
                    log.info("eventType=ENDPOINT_FREQUENCY_SNAPSHOT endpoint=\"{}\" hits={}",
                            endpoint, counter.get()));
        }
        endpointFrequency.clear();
        ipFrequency.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IP resolution helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the real client IP, honouring standard reverse-proxy headers
     * (X-Forwarded-For, X-Real-IP) before falling back to
     * {@link HttpServletRequest#getRemoteAddr()}.
     *
     * Order of precedence mirrors nginx / AWS ALB header conventions.
     */
    public static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may be "client, proxy1, proxy2" — take the first (leftmost)
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Log line builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a structured, key=value log line for each field in the event.
     * This format is directly parseable by logstash-logback-encoder as a
     * flat JSON object when a structured appender is configured, and remains
     * readable as plaintext when it is not.
     */
    private String buildLogLine(SecurityAuditEvent event) {
        return String.format(
                "eventType=%s username=\"%s\" ipAddress=%s endpoint=\"%s\" " +
                "httpMethod=%s timestamp=%s details=\"%s\"",
                event.getEventType(),
                event.getUsername(),
                event.getIpAddress(),
                event.getEndpoint(),
                event.getHttpMethod(),
                event.getTimestamp(),
                sanitize(event.getDetails())
        );
    }

    /**
     * Strips newline characters from free-text to prevent log injection attacks
     * (CWE-117 — Improper Output Neutralization for Logs).
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\n", "\\n").replace("\r", "\\r");
    }
}
