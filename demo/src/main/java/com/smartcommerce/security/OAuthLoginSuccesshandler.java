package com.smartcommerce.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smartcommerce.model.User;
import com.smartcommerce.model.UserRole;
import com.smartcommerce.repositories.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthLoginSuccesshandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Check if user already exists in DB; if not, create with CUSTOMER role
        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            // OAuth2 users have no password leave password blank
            newUser.setRole(UserRole.CUSTOMER);
            return userRepository.save(newUser);
        });

        // Generate JWT using the persisted user's details (includes correct role)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        String token = jwtTokenService.generateToken(userDetails);

        // Redirect to frontend with token
        response.sendRedirect("http://localhost:3000?token=" + token);
    }
}
