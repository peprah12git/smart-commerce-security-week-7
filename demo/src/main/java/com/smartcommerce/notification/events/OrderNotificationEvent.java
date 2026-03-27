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


//publisher is the component that creates and dispatches the event
//Listener listens for a specific type of even and performs an action when the event is published.