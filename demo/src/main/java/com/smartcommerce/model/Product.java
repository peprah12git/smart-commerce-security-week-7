package com.smartcommerce.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private int productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "category_id")
    private int categoryId;

    @Transient
    private String categoryName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Transient
    private int quantityAvailable;

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL)
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL)
    private List<Review> reviews;

    public Product(String productName, String description, BigDecimal price, int categoryId) {
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = this.quantityAvailable + quantityAvailable;
    }

    public int getQuantity() {
        return getQuantityAvailable();
    }

    public void setQuantity(int quantity) {
        setQuantityAvailable(quantity);
    }

    @Override
    public String toString() {
        return "Product{id=" + productId + ", name='" + productName + "', price=" + price + "}";
    }
}

