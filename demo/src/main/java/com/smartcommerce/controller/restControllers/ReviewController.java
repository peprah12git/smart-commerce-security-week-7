package com.smartcommerce.controller.restControllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.CreateReviewDTO;
import com.smartcommerce.dtos.request.UpdateReviewDTO;
import com.smartcommerce.model.Product;
import com.smartcommerce.model.Review;
import com.smartcommerce.model.User;
import com.smartcommerce.security.SecurityUtils;
import com.smartcommerce.service.serviceInterface.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Product review management API")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Create a new review",
            description = "Submits a review for a product. The reviewer is the authenticated user.")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF')")
    @PostMapping
    public ResponseEntity<Review> createReview(
            @Valid @RequestBody CreateReviewDTO dto) {
        User currentUser = securityUtils.getCurrentUser();
        Product product = new Product();
        product.setProductId(dto.productId());
        Review review = new Review();
        review.setUser(currentUser);
        review.setProduct(product);
        review.setRating(dto.rating());
        review.setComment(dto.comment());
        Review created = reviewService.createReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get review by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable int id) {
        Review review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "Get all reviews for a product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProductId(@PathVariable int productId) {
        List<Review> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Get my reviews",
            description = "Retrieves all reviews submitted by the authenticated user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<Review>> getMyReviews() {
        int userId = securityUtils.getCurrentUserId();
        List<Review> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Get all reviews by a user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getReviewsByUserId(
            @PathVariable int userId) {
        List<Review> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Get all reviews")
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        List<Review> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Update a review")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @PathVariable int id,
            @Valid @RequestBody UpdateReviewDTO dto) {
        Review updated = reviewService.updateReview(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete a review")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable int id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
