package com.smartcommerce.service.imp;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;

@Service
public class InvoiceService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float PAGE_MARGIN = 50f;
    private static final float LEADING = 16f;
    private static final int MAX_LINE_CHARS = 95;

    @Async("invoiceExecutor")
    public CompletableFuture<String> generateInvoiceAsync(Order order) {
        try {
            System.out.println("Generating invoice on thread: " + Thread.currentThread().getName());

            String filePath = generateInvoiceFile(order);

            return CompletableFuture.completedFuture(filePath);
        } catch (IOException | RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String generateInvoiceFile(Order order) throws IOException {
        Path invoicesDir = Paths.get("invoices");
        Files.createDirectories(invoicesDir);

        Path filePath = invoicesDir.resolve("order-" + order.getOrderId() + ".pdf");
        List<String> lines = buildInvoiceLines(order);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PAGE_MARGIN;
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                content.setLeading(LEADING);
                content.newLineAtOffset(PAGE_MARGIN, y);

                for (int i = 0; i < lines.size(); i++) {
                    if (i == 1) {
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    }
                    content.showText(lines.get(i));
                    if (i < lines.size() - 1) {
                        content.newLine();
                    }
                }
                content.endText();
            }

            document.save(filePath.toFile());
        }

        return filePath.toAbsolutePath().toString();
    }

    private List<String> buildInvoiceLines(Order order) {
        List<String> lines = new ArrayList<>();
        lines.add("INVOICE");
        lines.add("Order ID: " + order.getOrderId());
        lines.add("Status: " + safe(order.getStatus()));
        lines.add("Order Date: " + formatOrderDate(order));
        lines.add("Customer: " + customerName(order));
        lines.add("Email: " + customerEmail(order));
        lines.add("");
        lines.add("Items:");

        List<OrderItem> items = order.getOrderItems() == null ? List.of() : order.getOrderItems();
        if (items.isEmpty()) {
            lines.add("- No order items available");
        } else {
            for (OrderItem item : items) {
                String itemLine = String.format(
                        "- %s | qty=%d | unit=%s | subtotal=%s",
                        productName(item),
                        item.getQuantity(),
                        money(item.getUnitPrice()),
                        money(item.getSubtotal())
                );
                lines.addAll(wrap(itemLine, MAX_LINE_CHARS));
            }
        }

        lines.add("");
        lines.add("Total Amount: " + money(order.getTotalAmount()));
        return lines;
    }

    private List<String> wrap(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return List.of(text == null ? "" : text);
        }

        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines;
    }

    private String formatOrderDate(Order order) {
        if (order.getOrderDate() == null) {
            return "N/A";
        }
        return order.getOrderDate().toLocalDateTime().format(TS_FORMAT);
    }

    private String customerName(Order order) {
        if (order.getUser() == null || order.getUser().getName() == null) {
            return "N/A";
        }
        return order.getUser().getName();
    }

    private String customerEmail(Order order) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            return "N/A";
        }
        return order.getUser().getEmail();
    }

    private String productName(OrderItem item) {
        if (item == null || item.getProduct() == null || item.getProduct().getName() == null) {
            return "Unknown Product";
        }
        return item.getProduct().getName();
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }
}