package com.smartcommerce.service.serviceInterface;

import com.smartcommerce.notification.events.OrderNotificationEvent;

public interface EmailNotificationService {

    void sendOrderNotification(OrderNotificationEvent event);
}
