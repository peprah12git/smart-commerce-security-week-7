package com.smartcommerce.security.audit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles HTTP 403 Forbidden responses for authenticated principals who
 * attempt to access resources above their role level.
 *
 * Replaces Spring Security's default {@code AccessDeniedHandlerImpl} to:
 *   1. Emit a structured {@link SecurityEventType#ACCESS_DENIED} audit log
 *      via {@link SecurityAuditService}.
 *   2. Return a consistent JSON error body (matching the format used by
 *      {@code JwtAuthenticationEntryPoint} for 401 responses).
 *
 * Example scenario:
 *   A CUSTOMER-role user calls DELETE /api/products/42 (ADMIN only).
 *   Spring Security calls this handler after the JWT is validated but
 *   before the controller method executes.
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccessDeniedHandlerImpl(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // Identify the principal attempting the access
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "anonymous";

        // Emit audit event — O(1) log + O(1) map insert
        auditService.accessDenied(username, request);

        // Return a structured JSON 403 body
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", "You do not have permission to access this resource");
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
