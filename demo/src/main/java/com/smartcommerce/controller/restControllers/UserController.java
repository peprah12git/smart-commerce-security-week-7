package com.smartcommerce.controller.restControllers;

import com.smartcommerce.dtos.request.LoginRequestDTO;
import com.smartcommerce.dtos.request.RegisterRequestDTO;
import com.smartcommerce.dtos.response.LoginResponseDTO;
import com.smartcommerce.dtos.response.UserResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.User;
import com.smartcommerce.service.serviceInterface.UserService;
import com.smartcommerce.utils.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST Controller for User management
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management API — registration and account operations")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // Manual constructor for compatibility
    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    // Endpoint to register a new user
    @PostMapping
    public ResponseEntity<UserResponse> addUser(
            @Valid @RequestBody RegisterRequestDTO createUserDTO
    ) {
        User userToCreate = new User(
                createUserDTO.name(),
                createUserDTO.email(),
                createUserDTO.password(),
                createUserDTO.phone(),
                createUserDTO.address()
        );
        User user = userService.registration(userToCreate);
        UserResponse response = UserMapper.toUserResponse(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "User login", description = "Authenticates user with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        User user = userService.getUserByEmail(loginRequest.email());
        
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponseDTO response = new LoginResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                "Login successful"
        );
        return ResponseEntity.ok(response);
    }

}
