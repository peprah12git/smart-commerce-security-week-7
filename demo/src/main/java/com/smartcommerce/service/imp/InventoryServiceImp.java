package com.smartcommerce.service.imp;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.model.Inventory;
import com.smartcommerce.repositories.InventoryRepository;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;

/**
 * Service layer for Inventory entity
 * Handles business logic, validation, and caching of inventory operations
 */
@Service
public class InventoryServiceImp implements InventoryServiceInterface {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImp(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public boolean updateInventory(int productId, int quantity) {
        int updated = inventoryRepository.updateInventoryQuantity(productId, quantity);
        return updated > 0;
    }

    @Override
    @Cacheable(value = "inventory", key = "#productId")
    public Inventory getInventoryByProductId(int productId) {
        return inventoryRepository.findByProductProductId(productId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory")
    public Page<Inventory> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> getLowStockItems(int threshold, Pageable pageable) {
        return inventoryRepository.findLowStockItems(threshold, pageable);
    }

    @Override
    public List<Inventory> getLowStockItems(int threshold) {
        return inventoryRepository.findLowStockItems(threshold);
    }

    /**
     * Business logic: Check if product is in stock
     */
    @Override
    public boolean isInStock(int productId) {
        Inventory inv = getInventoryByProductId(productId);
        return inv != null && inv.getQuantityAvailable() > 0;
    }

    /**
     * Business logic: Check if sufficient quantity available
     */
    @Override
    public boolean hasEnoughStock(int productId, int requestedQuantity) {
        Inventory inv = getInventoryByProductId(productId);
        return inv != null && inv.getQuantityAvailable() >= requestedQuantity;
    }

    /**
     * Business logic: Reduce stock (for order processing)
     */
    @Override
    public boolean reduceStock(int productId, int quantity) {
        // Get fresh data from database, not cache
        Optional<Inventory> invOpt = inventoryRepository.findByProductProductId(productId);
        if (invOpt.isPresent()) {
            Inventory inv = invOpt.get();
            if (inv.getQuantityAvailable() >= quantity) {
                int newQuantity = inv.getQuantityAvailable() - quantity;
                return updateInventory(productId, newQuantity);
            }
        }
        return false;
    }

    /**
     * Business logic: Restock
     */
    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public boolean addStock(int productId, int quantity) {
        // Get fresh data from database, not cache
        Optional<Inventory> invOpt = inventoryRepository.findByProductProductId(productId);
        if (invOpt.isPresent()) {
            Inventory inv = invOpt.get();
            int newQuantity = inv.getQuantityAvailable() + quantity;
            return updateInventory(productId, newQuantity);
        }
        return false;
    }

    /**
     * Filtering: Out of stock items
     */
    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getOutOfStockItems() {
        return inventoryRepository.findByQuantityAvailable(0);
    }

    /**
     * Sorting by quantity
     */
    @Override
    public List<Inventory> sortByQuantity(boolean ascending) {
        Sort sort = ascending
                ? Sort.by("quantityAvailable").ascending()
                : Sort.by("quantityAvailable").descending();
        return inventoryRepository.findAll(sort);
    }

    /**
     * Update stock for a product (alias for updateInventory)
     */
    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public boolean updateStock(int productId, int quantity) {
        return updateInventory(productId, quantity);
    }

}
