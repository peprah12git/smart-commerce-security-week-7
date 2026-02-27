package com.smartcommerce.repositories;

import com.smartcommerce.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_ProductIdOrderByReviewDateDesc(int productId);
    
    List<Review> findByUser_UserIdOrderByReviewDateDesc(int userId);
    
    List<Review> findAllByOrderByReviewDateDesc();
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    Double calculateAverageRating(@Param("productId") int productId);
}
