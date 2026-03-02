package com.smartcommerce.service.serviceInterface;

import java.util.List;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.User;

/**
 * Service interface for User entity
 * Defines business operations related to users
 */
public interface UserService {

    /**
     * Creates a new user
     *
     * @param user User object to create
     * @return Created user
     * @throws DuplicateResourceException if email already exists
     * @throws BusinessException          if user creation fails
     */
    User createUser(User user);

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
}
