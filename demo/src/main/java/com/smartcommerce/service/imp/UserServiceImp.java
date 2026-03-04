package com.smartcommerce.service.imp;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#result.userId"),
            @CacheEvict(value = "userByEmail", key = "#result.email")
    })
    public User registration(User request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        // DSA Principle: BCrypt hashing (adaptive cost, salted)
        // passwordEncoder.encode() runs bcrypt KDF with 2^10 iterations + random salt.
        // The resulting 60-char string is safe to store; the raw password is never persisted.
        // Time complexity: O(1) — independent of dataset size, bounded by work factor.
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
            existingUser.setPassword(userDetails.getPassword());
        }

        // Update role only if provided
        if (userDetails.getRole() != null) {
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
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
