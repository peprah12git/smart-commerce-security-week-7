package com.smartcommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.model.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    Optional<Inventory> findByProductProductId(int productId);

    List<Inventory> findByQuantityAvailable(int quantity);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < :threshold")
    Page<Inventory> findLowStockItems(@Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < :threshold ORDER BY i.quantityAvailable ASC")
    List<Inventory> findLowStockItems(@Param("threshold") int threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantityAvailable = :quantity WHERE i.product.productId = :productId")
    int updateInventoryQuantity(@Param("productId") int productId, @Param("quantity") int quantity);

        @Query("""
       SELECT i.product.productId, COALESCE(SUM(i.quantityAvailable), 0)
       FROM Inventory i
       WHERE i.product.productId IN :productIds
       GROUP BY i.product.productId
       """)
    List<Object[]> sumStockForProducts(@Param("productIds") List<Integer> productIds);

}
