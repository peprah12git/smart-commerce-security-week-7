package com.smartcommerce.dtos.request;

import com.smartcommerce.validation.ValidPhone;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new user")
public record RegisterRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "Email address (must be unique)", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$",
                message = "Password must include uppercase, lowercase, number, and special character"
        )
        @Schema(description = "Account password (12+ chars with upper/lower/number/special)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @ValidPhone
        @Schema(description = "Phone number", example = "+1 (555) 123-4567")
        String phone,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        @Schema(description = "Mailing address", example = "123 Main St, Springfield, IL 62701")
        String address
) {
}