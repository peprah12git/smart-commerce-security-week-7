package com.smartcommerce.controller.restControllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.RegisterRequestDTO;
import com.smartcommerce.dtos.request.UpdateUserDTO;
import com.smartcommerce.dtos.response.UserResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.User;
import com.smartcommerce.model.UserRole;
import com.smartcommerce.service.serviceInterface.UserService;
import com.smartcommerce.utils.UserMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for user profile management.
 * Base URL: /api/users
 *
 * Login and logout are handled by AuthController at /api/auth/login and /api/auth/logout.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management — registration, profile viewing, and account updates")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register (public)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user", description = "Creates a new customer account. No authentication required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequestDTO request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setPhone(request.phone());
        user.setAddress(request.address());

        User created = userService.registration(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toUserResponse(created));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Own profile (any authenticated user)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get my profile", description = "Returns the profile of the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getUserByEmail(principal.getUsername());
        return ResponseEntity.ok(UserMapper.toUserResponse(user));
    }

    @Operation(summary = "Update my profile", description = "Updates the profile of the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateUserDTO request) {
        User current = userService.getUserByEmail(principal.getUsername());
        User updated = buildUserFromUpdateDTO(request);
        User saved = userService.updateUser(current.getUserId(), updated);
        return ResponseEntity.ok(UserMapper.toUserResponse(saved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin-only endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all users", description = "Returns a list of all registered users. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = UserMapper.toUserResponseList(userService.getAllUsers());
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID", description = "Returns a specific user by their ID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable int id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserMapper.toUserResponse(user));
    }

    @Operation(summary = "Update a user by ID", description = "Updates a user's details. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable int id,
            @Valid @RequestBody UpdateUserDTO request) {
        User updated = buildUserFromUpdateDTO(request);
        User saved = userService.updateUser(id, updated);
        return ResponseEntity.ok(UserMapper.toUserResponse(saved));
    }

    @Operation(summary = "Delete a user by ID", description = "Permanently deletes a user account. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private User buildUserFromUpdateDTO(UpdateUserDTO dto) {
        User user = new User();
                if (dto.name() != null && !dto.name().isBlank()) {
                        user.setName(dto.name());
                }
                if (dto.email() != null && !dto.email().isBlank()) {
                        user.setEmail(dto.email());
                }
                if (dto.phone() != null && !dto.phone().isBlank()) {
                        user.setPhone(dto.phone());
                }
                if (dto.address() != null && !dto.address().isBlank()) {
                        user.setAddress(dto.address());
                }
        if (dto.role() != null && !dto.role().isBlank()) {
            user.setRole(UserRole.valueOf(dto.role()));
        }
        return user;
    }
}
