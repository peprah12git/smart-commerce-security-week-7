package com.smartcommerce.security.audit;

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

    BRUTE_FORCE_DETECTED,

    /** Endpoint hit-rate for a single IP exceeded the configured threshold. */
    HIGH_FREQUENCY_REQUEST
}
