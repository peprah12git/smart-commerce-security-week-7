package com.smartcommerce.controller.restControllers;

import com.smartcommerce.dtos.request.UpdateReviewDTO;
import com.smartcommerce.model.Review;
import com.smartcommerce.service.serviceInterface.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Product review management API")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Create a new review")
    @PostMapping
    public ResponseEntity<Review> createReview(
            @RequestBody Review review) {
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
    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @PathVariable int id,
            @Valid @RequestBody UpdateReviewDTO dto) {
        Review updated = reviewService.updateReview(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete a review")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable int id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
