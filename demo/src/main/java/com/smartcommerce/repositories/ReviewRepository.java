package com.smartcommerce.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcommerce.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.product WHERE r.product.productId = :productId ORDER BY r.reviewDate DESC")
    List<Review> findByProduct_ProductIdOrderByReviewDateDesc(@Param("productId") int productId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.product WHERE r.user.userId = :userId ORDER BY r.reviewDate DESC")
    List<Review> findByUser_UserIdOrderByReviewDateDesc(@Param("userId") int userId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.product ORDER BY r.reviewDate DESC")
    List<Review> findAllByOrderByReviewDateDesc();
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    Double calculateAverageRating(@Param("productId") int productId);
}
