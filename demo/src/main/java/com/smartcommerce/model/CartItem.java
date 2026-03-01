package com.smartcommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "CartItems",
        indexes = {
                @Index(name = "idx_cart_user", columnList = "user_id"),
                @Index(name = "idx_cart_product", columnList = "product_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_product", columnNames = {"user_id", "product_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private int cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Timestamp addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public int getProductId() {
        return product != null ? product.getProductId() : 0;
    }

    public int getUserId() {
        return user != null ? user.getUserId() : 0;
    }

    public BigDecimal getSubtotal() {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
