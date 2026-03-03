package com.smartcommerce.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login response with user details")
public class LoginResponseDTO {
    @Schema(description = "User ID", example = "1")
    private int userId;

    @Schema(description = "User name", example = "John Doe")
    private String name;

    @Schema(description = "User email", example = "admin@test.com")
    private String email;

    @Schema(description = "User role", example = "ADMIN")
    private String role;

    @Schema(description = "Signed JWT token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Login message", example = "Login successful")
    private String message;
}
