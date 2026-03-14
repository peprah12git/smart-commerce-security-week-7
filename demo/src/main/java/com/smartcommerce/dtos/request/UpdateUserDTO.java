package com.smartcommerce.dtos.request;

import com.smartcommerce.validation.ValidPhone;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for updating an existing user")
public record UpdateUserDTO(

        @Schema(description = "Updated full name", example = "Jane Doe")
        String name,

        @Schema(description = "Updated email address", example = "jane.doe@example.com")
        String email,

        @ValidPhone
        @Schema(description = "Updated phone number", example = "+1 (555) 987-6543")
        String phone,

        @Schema(description = "Updated mailing address", example = "456 Oak Ave, Chicago, IL 60601")
        String address,

        @Pattern(regexp = "^(CUSTOMER|ADMIN)$", message = "Role must be either CUSTOMER or ADMIN")
        @Schema(description = "User role", example = "CUSTOMER", allowableValues = {"CUSTOMER", "ADMIN"})
        String role
) {
}