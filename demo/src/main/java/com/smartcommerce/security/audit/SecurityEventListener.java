package com.smartcommerce.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**

 */
@Component
public class SecurityEventListener {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

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
