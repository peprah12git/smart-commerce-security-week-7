package com.smartcommerce.utils;

import com.smartcommerce.dtos.response.UserResponse;
import com.smartcommerce.model.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility class for User entity and DTOs
 */
public class UserMapper {

    /**
     * Converts User entity to UserResponse DTO
     */
    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

    /**
     * Converts list of User entities to list of UserResponse DTOs
     */
    public static List<UserResponse> toUserResponseList(List<User> users) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(UserMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}