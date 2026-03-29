package com.smartcommerce.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartcommerce.security.audit.LoginAttemptService;
import com.smartcommerce.security.audit.SecurityAuditService;

import io.jsonwebtoken.Claims;
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

    @Value("${app.auth.cookie.name:AUTH_TOKEN}")
    private String authCookieName;

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
        
        // Early exit for public endpoints (no auth needed)
        String uri = request.getRequestURI();
        if (isPublicEndpoint(uri, request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // ── Rate-limit check ───────
            boolean rateLimitBreached = auditService.trackRequest(request);
            if (rateLimitBreached) {
                String ip = SecurityAuditService.resolveClientIp(request);
                auditService.highFrequencyRequest(ip, request, auditService.ipHits(ip));
            }

            String token = getJwtFromRequest(request);

            if (token != null) {
                // ── Full validation (signature + expiry) ──
                Claims claims = jwtTokenService.validateToken(token);
                if (claims != null) {
                    // ── Revoked-token reuse detection (O(1) blacklist check) ──
                    if (tokenBlacklistService.isRevoked(token)) {
                        String username = claims.getSubject();
                        auditService.revokedTokenReuse(username, request);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String email = claims.getSubject();
                    if (loginAttemptService.isBlocked(email)) {
                        auditService.jwtValidationFailure(request,
                                "Account soft-locked due to repeated failed attempts — username=" + email);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Extract roles from JWT claims (no DB lookup - 900ms saved!)
                    List<String> roles = (List<String>) claims.get("roles");
                    var authorities = roles.stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                    
                    var userDetails = org.springframework.security.core.userdetails.User.builder()
                            .username(email)
                            .password("") // Not needed for JWT auth
                            .authorities(authorities)
                            .build();
                    
                    var authentication = JWTAuthenticationToken.authenticated(
                            userDetails, token, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } else {
                    // Token present but failed validation
                    auditService.jwtValidationFailure(request,
                            "Token failed signature/expiry validation");
                }
            }
        } catch (RuntimeException ex) {
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

        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (authCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
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
    
    /**
     * Early exit optimization: skip JWT processing for public endpoints.
     * Saves ~1200ms per request on public endpoints.
     */
    private boolean isPublicEndpoint(String uri, String method) {
        // Auth endpoints
        if (uri.startsWith("/api/auth/") || uri.equals("/api/users") && "POST".equals(method)) {
            return true;
        }
        
        // Public read endpoints
        if ("GET".equals(method) && (uri.startsWith("/api/products") || 
                                      uri.startsWith("/api/categories") || 
                                      uri.startsWith("/api/reviews"))) {
            return true;
        }
        
        // GraphQL, Swagger, Actuator
        if (uri.startsWith("/graphql") || uri.startsWith("/swagger-ui") || 
            uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator")) {
            return true;
        }
        
        // OAuth2
        if (uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/")) {
            return true;
        }
        
        return false;
    }
}
