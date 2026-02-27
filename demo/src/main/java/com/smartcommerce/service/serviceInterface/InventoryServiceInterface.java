package com.smartcommerce.service.serviceInterface;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Inventory;

/**
 * Service interface for Inventory entity
 * Defines business operations related to inventory management
 */
public interface InventoryServiceInterface {

    /**
     * Updates the inventory quantity for a product
     *
     * @param productId the product ID
     * @param quantity the new quantity
     * @return true if update was successful
     * @throws ResourceNotFoundException if product not found
     * @throws BusinessException if update fails
     */
    boolean updateInventory(int productId, int quantity);

    /**
     * Retrieves inventory information for a specific product
     *
     * @param productId the product ID
     * @return inventory information
     * @throws ResourceNotFoundException if inventory not found
     */
    Inventory getInventoryByProductId(int productId);

    /**
     * Retrieves all inventory records (paginated)
     *
     * @param pageable Pagination and sorting parameters
     * @return paginated inventory items
     */
    Page<Inventory> getAllInventory(Pageable pageable);


    /**
     * Retrieves inventory items below a stock threshold (paginated)
     *
     * @param threshold the minimum quantity threshold
     * @param pageable  Pagination and sorting parameters
     * @return paginated low stock inventory items
     */
    Page<Inventory> getLowStockItems(int threshold, Pageable pageable);

    /**
     * Retrieves inventory items below a stock threshold
     *
     * @param threshold the minimum quantity threshold
     * @return list of low stock inventory items
     */
    List<Inventory> getLowStockItems(int threshold);

    /**
     * Checks if a product is in stock (quantity > 0)
     *
     * @param productId the product ID
     * @return true if in stock, false otherwise
     */
    boolean isInStock(int productId);

    /**
     * Checks if sufficient quantity is available for a product
     *
     * @param productId the product ID
     * @param requestedQuantity the quantity needed
     * @return true if enough stock available, false otherwise
     */
    boolean hasEnoughStock(int productId, int requestedQuantity);

    /**
     * Reduces stock for a product (for order processing)
     *
     * @param productId the product ID
     * @param quantity the quantity to reduce
     * @return true if reduction was successful
     * @throws ResourceNotFoundException if product not found
     * @throws BusinessException if insufficient stock
     */
    boolean reduceStock(int productId, int quantity);

    /**
     * Adds stock to a product (for restocking)
     *
     * @param productId the product ID
     * @param quantity the quantity to add
     * @return true if addition was successful
     * @throws ResourceNotFoundException if product not found
     * @throws BusinessException if operation fails
     */
    boolean addStock(int productId, int quantity);

    /**
     * Retrieves all out-of-stock items (quantity = 0)
     *
     * @return list of out-of-stock inventory items
     */
    List<Inventory> getOutOfStockItems();

    /**
     * Retrieves inventory sorted by quantity
     *
     * @param ascending true for ascending order, false for descending
     * @return sorted list of inventory items
     */
    List<Inventory> sortByQuantity(boolean ascending);

    /**
     * Updates stock for a product (alias for updateInventory)
     *
     * @param productId the product ID
     * @param quantity the new quantity
     * @return true if update was successful
     */
    boolean updateStock(int productId, int quantity);
}
