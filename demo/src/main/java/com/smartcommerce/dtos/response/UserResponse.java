package com.smartcommerce.dtos.response;

import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information returned by the API (password excluded)")
public class UserResponse {

    @Schema(description = "Unique user identifier", example = "1")
    private int userId;

    @Schema(description = "Full name", example = "John Doe")
    private String name;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+1 (555) 123-4567")
    private String phone;

    @Schema(description = "Mailing address", example = "123 Main St, Springfield, IL 62701")
    private String address;

    @Schema(description = "User role", example = "CUSTOMER", allowableValues = {"CUSTOMER", "ADMIN"})
    private String role;

    @Schema(description = "Account creation timestamp")
    private Timestamp createdAt;

    // Manual setters for compatibility
    public void setUserId(int userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
