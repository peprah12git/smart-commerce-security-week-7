package com.smartcommerce.security.audit;

/**
 * Canonical vocabulary of security events that the audit system records.
 *
 * Using an enum (rather than plain strings) gives us:
 *   • Compile-time exhaustiveness — no typo-ridden event names in logs.
 *   • Easy filtering in log aggregators (Splunk, ELK) via exact-match on
 *     the "eventType" JSON field.
 *   • A single source of truth for dashboards and alerting rules.
 */
public enum SecurityEventType {

    // ── Authentication ───────────────────────────────────────────────────────
    /** User presented correct credentials and received a JWT. */
    LOGIN_SUCCESS,

    /** User presented wrong credentials (bad password, unknown email, etc.). */
    LOGIN_FAILURE,

    /** User explicitly revoked their own JWT. */
    LOGOUT,

    // ── Token ────────────────────────────────────────────────────────────────
    /** Bearer token failed signature, expiry, or blacklist check. */
    JWT_VALIDATION_FAILURE,

    /** A blacklisted (revoked) token was presented again. */
    REVOKED_TOKEN_REUSE,

    // ── Authorisation ────────────────────────────────────────────────────────
    /** Authenticated principal tried to access a resource above their role. */
    ACCESS_DENIED,

    // ── Anomaly / Threat Detection ───────────────────────────────────────────
    /**
     * Raised when a user exceeds the failed-login threshold within the
     * configured time window.  Indicates a possible brute-force attempt.
     */
    BRUTE_FORCE_DETECTED,

    /** Endpoint hit-rate for a single IP exceeded the configured threshold. */
    HIGH_FREQUENCY_REQUEST
}
