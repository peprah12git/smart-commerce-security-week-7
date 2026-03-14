package com.smartcommerce.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smartcommerce.model.User;
import com.smartcommerce.model.UserRole;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthLoginSuccesshandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;

    @Value("${app.auth.oauth.success-redirect-url:http://localhost:3000/login?oauth=success}")
    private String oauthSuccessRedirectUrl;

    @Value("${app.auth.cookie.name:AUTH_TOKEN}")
    private String authCookieName;

    @Value("${app.auth.cookie.secure:false}")
    private boolean authCookieSecure;

    @Value("${app.auth.cookie.max-age-seconds:86400}")
    private long authCookieMaxAgeSeconds;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        User user = new User();
        user.setEmail(email);

        UserRole role = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority != null && !authority.isBlank())
            .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
            .map(this::parseRole)
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(UserRole.CUSTOMER);

        user.setRole(role);

        String token = jwtTokenService.generateToken(user);

        ResponseCookie authCookie = ResponseCookie.from(authCookieName, token)
                .httpOnly(true)
                .secure(authCookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(authCookieMaxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());

        response.sendRedirect(resolveSuccessRedirectUrl(token));
    }

    private String resolveSuccessRedirectUrl(String token) {
        if (oauthSuccessRedirectUrl == null || oauthSuccessRedirectUrl.isBlank()) {
            return "http://localhost:3000/login";
        }

        if (oauthSuccessRedirectUrl.contains("/login?oauth=success")) {
            return oauthSuccessRedirectUrl.replace("/login?oauth=success", String.format("/home?token=%s", token));
        }

        return oauthSuccessRedirectUrl;
    }

    private Optional<UserRole> parseRole(String role) {
        try {
            return Optional.of(UserRole.valueOf(role));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
