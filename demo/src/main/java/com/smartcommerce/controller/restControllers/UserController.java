package com.smartcommerce.controller.restControllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for authenticated user profile management.
 * Base URL: /api/users
 *
 * Registration and login are handled by AuthController at /api/auth/register and /api/auth/login.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isA
@Tag(name = "Users", description = "User profile management  view and update your own account")
public class UserController {
    // Profile endpoints (get my profile, update profile, change password, etc.)
    // will be added here.
}
                "Login successful"
        );
        return ResponseEntity.ok(response);
    }

}

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenService.generateToken(userDetails);

        LoginResponseDTO response = new LoginResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Login successful"
        );
        return ResponseEntity.ok(response);
    }

}
