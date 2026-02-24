package com.smartcommerce.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcommerce.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // Non-paginated method (kept for backward compatibility)
    List<Category> findAllByOrderByCategoryName();
}
