package com.smartcommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Transient
    private String userName;

    @CreationTimestamp
    @Column(name = "order_date", updatable = false)
    private Timestamp orderDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Transient
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order(int userId, String status, BigDecimal totalAmount) {
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderItems = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Order{id=" + orderId + ", userId=" + userId + ", total=" + totalAmount + ", status='" + status + "'}";
    }
}
