package com.smartcommerce.service.imp;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.dtos.request.UpdateReviewDTO;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Review;
import com.smartcommerce.repositories.ReviewRepository;
import com.smartcommerce.service.serviceInterface.ReviewService;

import lombok.AllArgsConstructor;

@Service
@Transactional
@AllArgsConstructor
public class ReviewServiceImp implements ReviewService {

    private ReviewRepository reviewRepository;

    @Override
    public Review createReview(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }
        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            throw new BusinessException("Review comment is required");
        }

        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewById(int reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProductId(int productId) {
        return reviewRepository.findByProductIdOrderByReviewDateDesc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByUserId(int userId) {
        return reviewRepository.findByUserIdOrderByReviewDateDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Override
    public Review updateReview(int reviewId, UpdateReviewDTO dto) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        existing.setRating(dto.rating());
        existing.setComment(dto.comment());

        return reviewRepository.save(existing);
    }


    @Override
    public void deleteReview(int reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }

        reviewRepository.deleteById(reviewId);
    }
}
