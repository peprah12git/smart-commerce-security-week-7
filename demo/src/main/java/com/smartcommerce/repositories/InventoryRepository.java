package com.smartcommerce.repositories;

import com.smartcommerce.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    Optional<Inventory> findByProductId(int productId);
    
    // Paginated methods
    Page<Inventory> findAllByOrderByQuantityAvailableAsc(Pageable pageable);
    
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < :threshold")
    Page<Inventory> findLowStockItems(@Param("threshold") int threshold, Pageable pageable);
    
    // Non-paginated methods (kept for backward compatibility)
    List<Inventory> findAllByOrderByQuantityAvailableAsc();
    
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < :threshold ORDER BY i.quantityAvailable ASC")
    List<Inventory> findLowStockItems(@Param("threshold") int threshold);
    
    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantityAvailable = :quantity WHERE i.productId = :productId")
    int updateInventoryQuantity(@Param("productId") int productId, @Param("quantity") int quantity);
}
