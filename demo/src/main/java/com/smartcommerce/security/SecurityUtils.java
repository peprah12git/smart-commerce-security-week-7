package com.smartcommerce.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.model.User;
import com.smartcommerce.service.serviceInterface.UserService;

/**
 * Utility to extract the currently authenticated user from the SecurityContext.
 * UserService.getUserByEmail is @Cacheable so this involves no extra DB round-trips
 * for requests that already touched user data.
 */
@Component
public class SecurityUtils {

    private final UserService userService;

    public SecurityUtils(UserService userService) {
        this.userService = userService;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("No authenticated user found");
        }
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        return userService.getUserByEmail(email);
    }


    public int getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
