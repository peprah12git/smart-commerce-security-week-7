package com.smartcommerce.dtos.response;

import com.smartcommerce.model.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private int reviewId;
    private String comment;
    private int rating;
    private LocalDate reviewDate;
    private int userId;
    private String username;

    public static ReviewResponse from(Review r) {
        ReviewResponse dto = new ReviewResponse();
        dto.setReviewId(r.getReviewId());
        dto.setComment(r.getComment());
        dto.setRating(r.getRating());
        dto.setReviewDate(r.getReviewDate().toLocalDateTime().toLocalDate());
        dto.setUserId(r.getUser().getUserId());
        dto.setUsername(r.getUser().getName()); // adjust getter if needed
        return dto;
    }
}
