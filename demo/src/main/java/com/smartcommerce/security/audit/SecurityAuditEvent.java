package com.smartcommerce.security.audit;

import java.time.Instant;

/**
 * Immutable value object that represents a single captured security event.
 *
 * Design decisions
 * ────────────────
 * • All fields are set once at construction — thread-safe by design.
 * • Uses {@link Instant} (UTC epoch) instead of LocalDateTime so that
 *   log aggregators (ELK, Splunk, Datadog) can parse and sort correctly
 *   without timezone conversion issues.
 * • The {@code details} free-text field allows callers to attach context
 *   (e.g. "bad password", "token expired", "IP geo-blocked") without
 *   polluting the fixed schema.
 *
 * JSON output example (produced by SecurityAuditService):
 * <pre>
 * {
 *   "eventType"   : "LOGIN_FAILURE",
 *   "username"    : "alice@example.com",
 *   "ipAddress"   : "192.168.1.42",
 *   "endpoint"    : "/api/auth/login",
 *   "httpMethod"  : "POST",
 *   "timestamp"   : "2026-03-04T14:22:07.341Z",
 *   "details"     : "Bad credentials"
 * }
 * </pre>
 */
public final class SecurityAuditEvent {

    private final SecurityEventType eventType;
    private final String username;
    private final String ipAddress;
    private final String endpoint;
    private final String httpMethod;
    private final Instant timestamp;
    private final String details;

    private SecurityAuditEvent(Builder builder) {
        this.eventType  = builder.eventType;
        this.username   = builder.username;
        this.ipAddress  = builder.ipAddress;
        this.endpoint   = builder.endpoint;
        this.httpMethod = builder.httpMethod;
        this.timestamp  = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.details    = builder.details;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public SecurityEventType getEventType()  { return eventType;  }
    public String getUsername()              { return username;   }
    public String getIpAddress()             { return ipAddress;  }
    public String getEndpoint()              { return endpoint;   }
    public String getHttpMethod()            { return httpMethod; }
    public Instant getTimestamp()            { return timestamp;  }
    public String getDetails()               { return details;    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder — fluent, null-safe construction
    // ─────────────────────────────────────────────────────────────────────────

    public static Builder builder(SecurityEventType eventType) {
        return new Builder(eventType);
    }

    public static final class Builder {

        private final SecurityEventType eventType;
        private String  username   = "anonymous";
        private String  ipAddress  = "unknown";
        private String  endpoint   = "";
        private String  httpMethod = "";
        private Instant timestamp;
        private String  details    = "";

        private Builder(SecurityEventType eventType) {
            this.eventType = eventType;
        }

        public Builder username(String username) {
            this.username = username != null ? username : "anonymous";
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress != null ? ipAddress : "unknown";
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint != null ? endpoint : "";
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod != null ? httpMethod : "";
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder details(String details) {
            this.details = details != null ? details : "";
            return this;
        }

        public SecurityAuditEvent build() {
            return new SecurityAuditEvent(this);
        }
    }
}
