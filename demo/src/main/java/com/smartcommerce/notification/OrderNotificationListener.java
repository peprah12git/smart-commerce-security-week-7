package com.smartcommerce.notification;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final EmailNotificationService emailNotificationService;
    // Track which orders have been notified to prevent duplicates (atomic, thread-safe)
    private final ConcurrentHashMap<Integer, Boolean> notificationSent = new ConcurrentHashMap<>();

    public OrderNotificationListener(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @Async("notificationExecutor")
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderNotification(OrderNotificationEvent event) {
        try {
            // Check if notification has already been sent (atomic, thread-safe)
            if (notificationSent.putIfAbsent(event.orderId(), true) != null) {
                log.info("Email notification already sent for orderId={} type={}, skipping duplicate",
                        event.orderId(), event.notificationType());
                return;
            }
            
            log.info("email notification started");
            emailNotificationService.sendOrderNotification(event);
        } catch (Exception ex) {
            log.error("Failed to send async order notification for orderId={} type={}: {}",
                    event.orderId(), event.notificationType(), ex.getMessage(), ex);
        }
    }
}
