package com.smartcommerce.controller.restControllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.LoginRequestDTO;
import com.smartcommerce.dtos.response.LoginResponseDTO;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.model.User;
import com.smartcommerce.security.CustomUserDetailsService;
import com.smartcommerce.security.JwtTokenService;
import com.smartcommerce.service.serviceInterface.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication API — login and token generation")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(UserService userService,
                          JwtTokenService jwtTokenService,
                          CustomUserDetailsService customUserDetailsService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Operation(summary = "Login", description = "Authenticates user and returns a signed JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT token returned",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // 1. Authenticate via AuthenticationManager (throws on bad credentials)
        User user = userService.login(loginRequest.email(), loginRequest.password());

        // 2. Load UserDetails (needed by JwtTokenService to build the token)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        // 3. Generate signed JWT containing username, roles, and expiry
        String token = jwtTokenService.generateToken(userDetails);

        // 4. Build response with token
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
