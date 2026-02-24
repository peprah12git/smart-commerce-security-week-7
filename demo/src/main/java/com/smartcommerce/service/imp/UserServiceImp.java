package com.smartcommerce.service.imp;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.dtos.response.LoginResponse;
import com.smartcommerce.dtos.response.UserResponse;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.User;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.UserService;
import com.smartcommerce.utils.UserMapper;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;

/**
 * Service layer for User entity
 * Handles business logic, validation, and orchestration of user operations
 */
@Service
@Transactional
@AllArgsConstructor
public class UserServiceImp implements UserService {

    private UserRepository userRepository;
    private com.smartcommerce.security.JwtUtil jwtUtil;

    /**
     * Creates a new user
     *
     * @param user User object to create
     * @return Created user
     * @throws DuplicateResourceException if email already exists
     * @throws BusinessException          if user creation fails
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "user", key = "#result.userId"),
        @CacheEvict(value = "userByEmail", key = "#result.email")
    })
    public User createUser(User user) {
        // Validate input
        validateUser(user);

        // Check for duplicate email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }

        // Set default role if not provided
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("CUSTOMER");
        }

        // Hash the password before saving
        String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());
        user.setPassword(hashedPassword);

        // Save and return user
        return userRepository.save(user);
    }

    /**
     * Retrieves all users
     *
     * @return List of all users
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'all'")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by ID
     *
     * @param userId User ID
     * @return User object
     * @throws ResourceNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#userId")
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    /**
     * Retrieves a user by email
     *
     * @param email Email address
     * @return User object
     * @throws ResourceNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userByEmail", key = "#email")
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email cannot be empty");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Updates an existing user
     *
     * @param userId      User ID to update
     * @param userDetails Updated user details
     * @return Updated user
     * @throws ResourceNotFoundException  if user not found
     * @throws DuplicateResourceException if email already exists for another user
     * @throws BusinessException          if update fails
     */
    @Override
    @Caching(
        put = {@CachePut(value = "user", key = "#userId"), @CachePut(value = "userByEmail", key = "#result.email")},
        evict = @CacheEvict(value = "users", allEntries = true)
    )
    public User updateUser(int userId, User userDetails) {
        // Check if user exists
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate updated details
        validateUser(userDetails);

        // Check for duplicate email (if email is being changed)
        if (!existingUser.getEmail().equals(userDetails.getEmail())) {
            userRepository.findByEmail(userDetails.getEmail()).ifPresent(userWithEmail -> {
                if (userWithEmail.getUserId() != userId) {
                    throw new DuplicateResourceException("User", "email", userDetails.getEmail());
                }
            });
        }

        // Update user details
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setAddress(userDetails.getAddress());

        // Update password only if provided
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            // Hash the new password before updating
            String hashedPassword = BCrypt.withDefaults().hashToString(12, userDetails.getPassword().toCharArray());
            existingUser.setPassword(hashedPassword);
        }

        // Update role only if provided
        if (userDetails.getRole() != null && !userDetails.getRole().trim().isEmpty()) {
            existingUser.setRole(userDetails.getRole());
        }

        return userRepository.save(existingUser);
    }

    /**
     * Deletes a user
     *
     * @param userId User ID to delete
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException         if deletion fails
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "user", key = "#userId"),
        @CacheEvict(value = "userByEmail", allEntries = true)
    })
    public void deleteUser(int userId) {
        // Check if user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        userRepository.deleteById(userId);
    }

    /**
     * Validates user data
     *
     * @param user User to validate
     * @throws BusinessException if validation fails
     */
    private void validateUser(User user) {
        if (user == null) {
            throw new BusinessException("User cannot be null");
        }

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new BusinessException("User name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BusinessException("User email is required");
        }

        if (!isValidEmail(user.getEmail())) {
            throw new BusinessException("Invalid email format");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new BusinessException("User password is required");
        }

        if (user.getPassword().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters long");
        }
    }

    /**
     * Simple email validation
     *
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Authenticates a user with email and password
     * Generates JWT token with user role for RBAC
     *
     * @param email    User's email
     * @param password User's password
     * @return LoginResponse with JWT token and user info
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException         if credentials are invalid
     */
    @Override
    public LoginResponse login(String email, String password) {
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Password is required");
        }

        // Find user by email
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Verify password with BCrypt
        BCrypt.Result result = BCrypt.verifyer()
                .verify(password.toCharArray(), user.getPassword());
        if (!result.verified) {
            throw new BusinessException("Invalid credentials");
        }

        // Generate JWT token with user role
        String token = jwtUtil.generateToken(user);
        UserResponse userResponse = UserMapper.toUserResponse(user);

        return new LoginResponse(token, userResponse);
    }
}
