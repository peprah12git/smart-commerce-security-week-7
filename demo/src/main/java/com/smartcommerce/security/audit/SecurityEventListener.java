package com.smartcommerce.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to Spring Security application events published on the
 * {@link org.springframework.context.ApplicationEventPublisher}.
 *
 * Spring Security automatically publishes these events when an
 * {@link org.springframework.security.authentication.AuthenticationManager}
 * completes or fails.  No manual publisher code is required in the app.
 *
 * Events handled
 * ──────────────
 * • {@link AuthenticationSuccessEvent}
 *     Published after any successful authentication (form login, JWT pre-auth,
 *     OAuth2, etc.) — we use this as a secondary success signal alongside the
 *     AuthController's explicit loginSuccess() call.
 *
 * • {@link AbstractAuthenticationFailureEvent}
 *     Published for ANY authentication failure regardless of cause:
 *       – BadCredentialsException       → wrong password
 *       – UsernameNotFoundException     → email not found
 *       – DisabledException             → account disabled
 *       – LockedException               → account locked
 *       – CredentialsExpiredException   → password expired
 *     By listening to the abstract parent we catch all sub-types in one place.
 *
 * Note: These events are fired from AuthenticationManager processing.
 * The AuthController also calls the audit service directly for its own login/
 * logout events so that the HTTP request context (IP, URI, method) is available.
 * These listeners act as a safety net that captures events from OAuth2 flows
 * and other non-REST authentication paths.
 */
@Component
public class SecurityEventListener {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Fired for all successful authentications.
     * We skip it for the {@link InteractiveAuthenticationSuccessEvent}
     * sub-type (OAuth2 redirect flow) because OAuthLoginSuccesshandler
     * handles that path directly.
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = extractPrincipal(event.getAuthentication().getName());

        log.info("eventType=LOGIN_SUCCESS username=\"{}\" source=SpringSecurityEvent "
                + "authenticationType={}",
                principal,
                event.getAuthentication().getClass().getSimpleName());
    }

    /**
     * Fired for ALL authentication failures — catches every sub-type of
     * AbstractAuthenticationFailureEvent (BadCredentials, AccountDisabled, etc.).
     *
     * Note: This event gives us the exception type so we can categorise the
     * failure (wrong password vs. account locked vs. unknown user).
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String principal  = extractPrincipal(event.getAuthentication().getName());
        String failReason = event.getException().getClass().getSimpleName()
                + ": " + sanitize(event.getException().getMessage());

        log.warn("eventType=LOGIN_FAILURE username=\"{}\" source=SpringSecurityEvent " +
                 "reason=\"{}\"",
                principal, failReason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractPrincipal(String name) {
        return name != null ? name : "anonymous";
    }

    /** Prevent log injection (CWE-117). */
    private String sanitize(String input) {
        if (input == null) return "null";
        return input.replace("\n", "\\n").replace("\r", "\\r");
    }
}
