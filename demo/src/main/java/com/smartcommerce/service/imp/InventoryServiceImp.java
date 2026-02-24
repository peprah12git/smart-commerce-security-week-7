package com.smartcommerce.service.imp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private Map<Integer, Inventory> inventoryCache;
    private long lastCacheUpdate;
    private static final long CACHE_VALIDITY = 120000; // 2 minutes (inventory changes frequently)

    @Autowired
    public InventoryServiceImp(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryCache = new HashMap<>();
        this.lastCacheUpdate = 0;
    }

    @Override
    public boolean updateInventory(int productId, int quantity) {
        int updated = inventoryRepository.updateInventoryQuantity(productId, quantity);
        if (updated > 0) {
            invalidateCache();
            return true;
        }
        return false;
    }

    @Override
    public Inventory getInventoryByProductId(int productId) {
        if (!inventoryCache.isEmpty() && inventoryCache.containsKey(productId)) {
            long now = System.currentTimeMillis();
            if ((now - lastCacheUpdate) < CACHE_VALIDITY) {
                return inventoryCache.get(productId);
            }
        }
        return inventoryRepository.findByProductId(productId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> getAllInventory(Pageable pageable) {
        // For paginated results, skip cache and query database directly
        return inventoryRepository.findAll(pageable);
    }

    @Override
    public List<Inventory> getAllInventory() {
        long now = System.currentTimeMillis();

        if (!inventoryCache.isEmpty() && (now - lastCacheUpdate) < CACHE_VALIDITY) {
            System.out.println("✓ Inventory from cache");
            return new ArrayList<>(inventoryCache.values());
        }

        System.out.println("✗ Fetching inventory from database");
        List<Inventory> items = inventoryRepository.findAll();
        inventoryCache = items.stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));
        lastCacheUpdate = now;
        return new ArrayList<>(inventoryCache.values());
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
        Optional<Inventory> invOpt = inventoryRepository.findByProductId(productId);
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
    public boolean addStock(int productId, int quantity) {
        // Get fresh data from database, not cache
        Optional<Inventory> invOpt = inventoryRepository.findByProductId(productId);
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
    public List<Inventory> getOutOfStockItems() {
        return getAllInventory().stream()
                .filter(i -> i.getQuantityAvailable() == 0)
                .collect(Collectors.toList());
    }

    /**
     * Sorting by quantity
     */
    @Override
    public List<Inventory> sortByQuantity(boolean ascending) {
        List<Inventory> items = getAllInventory();
        if (ascending) {
            items.sort(Comparator.comparing(Inventory::getQuantityAvailable));
        } else {
            items.sort(Comparator.comparing(Inventory::getQuantityAvailable).reversed());
        }
        return items;
    }

    /**
     * Update stock for a product (alias for updateInventory)
     */
    @Override
    public boolean updateStock(int productId, int quantity) {
        return updateInventory(productId, quantity);
    }

    private void invalidateCache() {
        inventoryCache.clear();
        lastCacheUpdate = 0;
    }
}