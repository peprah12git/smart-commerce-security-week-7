package com.smartcommerce.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartcommerce.security.audit.LoginAttemptService;
import com.smartcommerce.security.audit.SecurityAuditService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executes once per request and gates every protected endpoint.

 * Audit events emitted:
 *   • JWT_VALIDATION_FAILURE — token present but invalid (signature, expiry)
 *   • REVOKED_TOKEN_REUSE    — token is specifically in the blacklist
 *   • HIGH_FREQUENCY_REQUEST — IP has exceeded the per-minute rate limit
 */
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityAuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

    public JWTAuthenticationFilter(JwtTokenService jwtTokenService,
                                    CustomUserDetailsService customUserDetailsService,
                                    SecurityAuditService auditService,
                                    LoginAttemptService loginAttemptService,
                                    TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenService        = jwtTokenService;
        this.customUserDetailsService = customUserDetailsService;
        this.auditService           = auditService;
        this.loginAttemptService    = loginAttemptService;
        this.tokenBlacklistService  = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // ── Rate-limit check ─────────────────────────────────────────────
            boolean rateLimitBreached = auditService.trackRequest(request);
            if (rateLimitBreached) {
                String ip = SecurityAuditService.resolveClientIp(request);
                auditService.highFrequencyRequest(ip, request, auditService.ipHits(ip));
            }

            String token = getJwtFromRequest(request);

            if (token != null) {
                // ── Revoked-token reuse detection (O(1) blacklist check) ──────
                if (tokenBlacklistService.isRevoked(token)) {
                    String username = tryExtractUsername(token);
                    auditService.revokedTokenReuse(username, request);
                    // Do NOT populate SecurityContext — let the request fail as 401
                    filterChain.doFilter(request, response);
                    return;
                }

                // ── Full validation (signature + expiry + blacklist) ──────────
                if (jwtTokenService.validateToken(token)) {
                    String email = jwtTokenService.getEmailFromToken(token);

                    // Brute-force soft-lock check — reject even valid JWTs while
                    // the account is locked to prevent token-based bypass.
                    if (loginAttemptService.isBlocked(email)) {
                        auditService.jwtValidationFailure(request,
                                "Account soft-locked due to repeated failed attempts — username=" + email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    var userDetails    = customUserDetailsService.loadUserByUsername(email);
                    var authentication = new JWTAuthenticationToken(
                            userDetails, token, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } else {
                    // Token present but failed validation
                    auditService.jwtValidationFailure(request,
                            "Token failed signature/expiry validation");
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("[AUDIT] JWT filter exception uri={} error={}",
                    request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /** Best-effort username extraction (token may be invalid — swallow exceptions). */
    private String tryExtractUsername(String token) {
        try {
            return jwtTokenService.getEmailFromToken(token);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
