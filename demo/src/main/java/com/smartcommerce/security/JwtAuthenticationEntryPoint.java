package com.smartcommerce.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.security.audit.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles HTTP 401 Unauthorized responses when a request arrives at a
 * protected endpoint with no token, an invalid token, or an expired token.
 *
 * Also emits a JWT_VALIDATION_FAILURE audit event so that all 401 outcomes
 * are tracked regardless of whether the request carried a token header.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityAuditService auditService;

    public JwtAuthenticationEntryPoint(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Audit: log every 401 — covers missing-token, expired-token, and post-filter
        // JWT failures that resulted in an unauthenticated SecurityContext.
        auditService.jwtValidationFailure(request,
                "Unauthenticated request reached entry point: " + authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "Access denied — valid JWT token required");
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
