package com.smartcommerce.notification.events;

import com.smartcommerce.notification.OrderNotificationType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record OrderNotificationEvent(
        Integer orderId,
        String customerName,
        String customerEmail,
        String orderStatus,
        BigDecimal totalAmount,
        Timestamp orderDate,
        OrderNotificationType notificationType
) {}

