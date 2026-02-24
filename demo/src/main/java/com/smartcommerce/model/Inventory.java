package com.smartcommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "Inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private int inventoryId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Transient
    private String productName;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    public Inventory(int inventoryId, int productId, int quantityAvailable) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.quantityAvailable = quantityAvailable;
    }

    public boolean isLowStock(int threshold) {
        return quantityAvailable < threshold;
    }
}
