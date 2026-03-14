package com.smartcommerce.service.imp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.User;
import com.smartcommerce.model.UserRole;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.UserService;

import lombok.AllArgsConstructor;

/**
 * Service layer for User entity
 * Handles business logic, validation, and orchestration of user operations
 */
@Service
@Transactional
@AllArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Creates a new user
     * @param request User object to create
     * @return Created user
     * @throws DuplicateResourceException if email already exists
     * @throws BusinessException          if user creation fails
     */
    @Override

    public User registration(User request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        validateRegistrationPassword(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        //Assigning default role
        user.setRole(UserRole.CUSTOMER);

        // Save the user to the database
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

        // Validate only fields provided for partial update
        validateUserForUpdate(userDetails);

        // Check for duplicate email (if email is being changed)
        if (hasText(userDetails.getEmail()) && !existingUser.getEmail().equals(userDetails.getEmail())) {
            userRepository.findByEmail(userDetails.getEmail()).ifPresent(userWithEmail -> {
                if (userWithEmail.getUserId() != userId) {
                    throw new DuplicateResourceException("User", "email", userDetails.getEmail());
                }
            });
        }

        // Update user details
        if (hasText(userDetails.getName())) {
            existingUser.setName(userDetails.getName());
        }
        if (hasText(userDetails.getEmail())) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (hasText(userDetails.getPhone())) {
            existingUser.setPhone(userDetails.getPhone());
        }
        if (hasText(userDetails.getAddress())) {
            existingUser.setAddress(userDetails.getAddress());
        }

        // Update role only if provided
        if (userDetails.getRole() != null) {
            existingUser.setRole(userDetails.getRole());
        }

        return userRepository.save(existingUser);
    }

    private void validateUserForUpdate(User user) {
        if (user == null) {
            throw new BusinessException("User cannot be null");
        }

        if (hasText(user.getName()) && user.getName().trim().length() < 2) {
            throw new BusinessException("User name must be at least 2 characters long");
        }

        if (hasText(user.getEmail()) && !isValidEmail(user.getEmail().trim())) {
            throw new BusinessException("Invalid email format");
        }

    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
     * Simple email validation
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    @Override
    @Transactional(readOnly = true)
    public User login(String email, String password) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );

            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        } catch (AuthenticationException authEx) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

            if (isLegacyPasswordMatch(user.getPassword(), password)) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                return user;
            }

            throw authEx;
        }
    }

    private boolean isLegacyPasswordMatch(String storedPassword, String rawPassword) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }

        // Ignore modern BCrypt hashes; those should be handled by AuthenticationManager.
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return false;
        }

        // Legacy plain-text match.
        if (storedPassword.equals(rawPassword)) {
            return true;
        }

        // Legacy SHA-256 hex match (common manual hashing pattern).
        return sha256Hex(rawPassword).equalsIgnoreCase(storedPassword);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private void validateRegistrationPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Password is required");
        }

        String complexityPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$";
        if (!password.matches(complexityPattern)) {
            throw new BusinessException("Password must be 12+ characters and include uppercase, lowercase, number, and special character");
        }
    }
}
