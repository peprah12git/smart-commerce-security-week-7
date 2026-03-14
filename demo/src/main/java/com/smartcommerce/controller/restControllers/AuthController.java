package com.smartcommerce.controller.restControllers;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.LoginRequestDTO;
import com.smartcommerce.dtos.response.LoginResponseDTO;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.model.User;
import com.smartcommerce.security.JwtTokenService;
import com.smartcommerce.security.audit.LoginAttemptService;
import com.smartcommerce.security.audit.SecurityAuditService;
import com.smartcommerce.service.serviceInterface.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication API â€” login, logout, and token management")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final SecurityAuditService auditService;
    private final LoginAttemptService loginAttemptService;

        @Value("${app.auth.cookie.name:AUTH_TOKEN}")
        private String authCookieName;

        @Value("${app.auth.cookie.secure:false}")
        private boolean authCookieSecure;

    public AuthController(UserService userService,
                          JwtTokenService jwtTokenService,
                          SecurityAuditService auditService,
                          LoginAttemptService loginAttemptService) {
        this.userService              = userService;
        this.jwtTokenService          = jwtTokenService;
        this.auditService             = auditService;
        this.loginAttemptService      = loginAttemptService;
    }


    @Operation(summary = "Login", description = "Authenticates user and returns a signed JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful â€” JWT token returned",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Account temporarily locked due to repeated failures",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                   HttpServletRequest request) {

        String email = loginRequest.email();

        // â”€â”€ Brute-force soft-lock check â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // O(1) ConcurrentHashMap lookup via LoginAttemptService.
        // If the account is locked, reject immediately â€” no DB query needed.
        if (loginAttemptService.isBlocked(email)) {
            auditService.loginFailure(email, request,
                    "Account is temporarily locked due to too many failed attempts");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Account temporarily locked. Please try again later.");
        }

        try {
            // 1. Authenticate via AuthenticationManager (BCrypt comparison inside)
            User user = userService.login(email, loginRequest.password());

                        // 2. Generate HMAC-SHA256 signed JWT from authenticated domain user
                        String token = jwtTokenService.generateToken(user);

            // 4. Audit: LOGIN_SUCCESS + reset failure counter
            auditService.loginSuccess(email, request);

            LoginResponseDTO response = new LoginResponseDTO(
                    user.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name(),
                    token,
                    "Login successful"
            );

            return ResponseEntity.ok(response);

                } catch (RuntimeException ex) {
            // 5. Audit: LOGIN_FAILURE â€” record failure, check if brute-force threshold crossed
            auditService.loginFailure(email, request, ex.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }


    /**
     * Revokes the caller's JWT by inserting it into the in-memory blacklist.
     * Security guarantee: even if the token has not yet expired, it will be
     * rejected by JwtTokenService.validateToken() â† blacklist check (Step 3).
     *
     * Also emits a LOGOUT audit event capturing the user, IP, and endpoint.
     *
     * @param authHeader the full Authorization header value ("Bearer &lt;token&gt;")
     * @param request    injected by Spring for IP / URI capture
     * @return 200 with a confirmation message; 400 if no Bearer token supplied
     */
    @Operation(
            summary = "Logout",
            description = "Revokes the caller's JWT by adding it to the in-memory blacklist (O(1) HashMap insert). "
                    + "Subsequent requests with this token are rejected at the filter layer without a DB query.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful â€” token revoked"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed Authorization header")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

                String token = extractToken(authHeader, request);

                if (token == null || token.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body("Missing or malformed authentication token");
                }

                // Resolve username for the audit log (best-effort — token is still valid here)
                String username = "unknown";
                try {
                        username = jwtTokenService.getEmailFromToken(token);
                } catch (Exception ignored) { }

                // O(1) SHA-256(token) inserted into ConcurrentHashMap blacklist
                jwtTokenService.revokeToken(token);

                // Emit structured LOGOUT audit event
                auditService.logout(username, request);

                ResponseCookie clearAuthCookie = ResponseCookie.from(authCookieName, "")
                                .httpOnly(true)
                                .secure(authCookieSecure)
                                .path("/")
                                .sameSite("Lax")
                                .maxAge(Duration.ZERO)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, clearAuthCookie.toString())
                                .body("Logout successful token has been revoked");
        }

        private String extractToken(String authHeader, HttpServletRequest request) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7);
        }

                if (request.getCookies() == null) {
                        return null;
                }

                for (var cookie : request.getCookies()) {
                        if (authCookieName.equals(cookie.getName())) {
                                return cookie.getValue();
                        }
                }

                return null;
    }
}
