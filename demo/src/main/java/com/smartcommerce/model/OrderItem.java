package com.smartcommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "OrderItems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    @Column(name = "order_id", nullable = false)
    private int orderId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Transient
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    public OrderItem(int orderId, int productId, int quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(new BigDecimal(quantity));
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        if (this.unitPrice != null) {
            this.subtotal = this.unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(new BigDecimal(this.quantity));
    }

    @Override
    public String toString() {
        return "OrderItem{id=" + orderItemId + ", orderId=" + orderId + ", productId=" + productId + ", qty=" + quantity + "}";
    }
}

