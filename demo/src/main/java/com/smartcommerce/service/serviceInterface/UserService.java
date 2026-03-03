package com.smartcommerce.service.serviceInterface;

import java.util.List;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * Service interface for User entity
 * Defines business operations related to users
 */
public interface UserService {

    /**
     * Creates a new user
     *
     * @param request User object to create
     * @return Created user
     * @throws DuplicateResourceException if email already exists
     * @throws BusinessException          if user creation fails
     */
   // User registration(User user);

    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "user", key = "#result.userId"),
        @CacheEvict(value = "userByEmail", key = "#result.email")
    })
    User registration(User request);

    /**
     * Retrieves all users
     *
     * @return List of all users
     */
    List<User> getAllUsers();

    /**
     * Retrieves a user by ID
     *
     * @param userId User ID
     * @return User object
     * @throws ResourceNotFoundException if user not found
     */
    User getUserById(int userId);

    /**
     * Retrieves a user by email
     *
     * @param email Email address
     * @return User object
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException         if email is invalid
     */
    User getUserByEmail(String email);

    /**
     * Updates an existing user
     *
     * @param userId      User ID to update
     * @param userDetails Updated user details
     * @return Updated user
     * @throws ResourceNotFoundException  if user not found
     * @throws DuplicateResourceException if email already exists
     * @throws BusinessException          if update fails
     */
    User updateUser(int userId, User userDetails);

    /**
     * Deletes a user
     *
     * @param userId User ID to delete
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException         if deletion fails
     */
    void deleteUser(int userId);

    /**
     * Authenticates user with email and password
     *
     * @param email User email
     * @param password User password
     * @return Authenticated User
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException if credentials are invalid
     */
    User login(String email, String password);
}
