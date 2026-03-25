package com.smartcommerce.notification;

public interface EmailNotificationService {

    void sendOrderNotification(OrderNotificationEvent event);
}
