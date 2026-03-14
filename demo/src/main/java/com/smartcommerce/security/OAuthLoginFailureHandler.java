package com.smartcommerce.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.auth.oauth.failure-redirect-url:http://localhost:3000/login}")
    private String oauthFailureRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "OAuth login failed";

        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        String separator = oauthFailureRedirectUrl.contains("?") ? "&" : "?";
        response.sendRedirect(oauthFailureRedirectUrl + separator + "error=" + encodedError);
    }
}