package com.smartcommerce.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request with email and password")
public record LoginRequestDTO(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User email", example = "admin@test.com")
    String email,

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "password123")
    String password
) {}
