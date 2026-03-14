package com.smartcommerce.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.smartcommerce.model.User;
import com.smartcommerce.model.UserRole;
import com.smartcommerce.repositories.UserRepository;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

        String email = extractEmail(attributes);
        String name = extractName(attributes, email);

        userRepository.findByEmail(email).ifPresentOrElse(existingUser -> {
            if (!hasText(existingUser.getName()) && hasText(name)) {
                existingUser.setName(name);
                userRepository.save(existingUser);
            }
        }, () -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setRole(UserRole.CUSTOMER);
            userRepository.save(newUser);
        });

        attributes.put("email", email);
        attributes.put("name", name);

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
    }

    private String extractEmail(Map<String, Object> attributes) {
        String email = asString(attributes.get("email"));
        if (!hasText(email)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                    "Email attribute is required from OAuth2 provider");
        }
        return email.trim();
    }

    private String extractName(Map<String, Object> attributes, String fallback) {
        String name = asString(attributes.get("name"));
        return hasText(name) ? name.trim() : fallback;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}