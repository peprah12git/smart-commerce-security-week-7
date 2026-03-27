package com.smartcommerce.service.imp;

import com.smartcommerce.notification.events.OrderNotificationEvent;
import com.smartcommerce.service.serviceInterface.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);

    private final JavaMailSender mailSender;

    public EmailNotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOrderNotification(OrderNotificationEvent event) {
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            log.warn("Skipping email notification because customer email is empty for orderId={}", event.orderId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.customerEmail());
        message.setSubject(buildSubject(event));
        message.setText(buildBody(event));

        mailSender.send(message);
        log.info("Order notification email sent: orderId={} type={} recipient={}",
                event.orderId(), event.notificationType(), event.customerEmail());
    }
//generating email subject line dynamically based on the type of order event
    private String buildSubject(OrderNotificationEvent event) {
        return switch (event.notificationType()) {
            case ORDER_CREATED -> "Order received #" + event.orderId();
            case ORDER_STATUS_UPDATED -> "Order status updated #" + event.orderId();
            case ORDER_CANCELLED -> "Order cancelled #" + event.orderId();
            case ORDER_CHECKOUT_COMPLETED -> "Checkout completed #" + event.orderId();
        };
    }

    private String buildBody(OrderNotificationEvent event) {
        String customerName = event.customerName() != null ? event.customerName() : "Customer";
        String status = event.orderStatus() != null ? event.orderStatus() : "pending";
        String amount = event.totalAmount() != null ? event.totalAmount().toPlainString() : "0";

        return "Hello " + customerName + ",\n\n"
                + "Your order update details are below:\n"
                + "Order ID: " + event.orderId() + "\n"
                + "Status: " + status + "\n"
                + "Total Amount: " + amount + "\n"
                + "Order Date: " + event.orderDate() + "\n\n"
                + "Thank you for shopping with us.";
    }
}
