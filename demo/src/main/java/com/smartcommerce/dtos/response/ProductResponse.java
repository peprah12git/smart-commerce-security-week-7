package com.smartcommerce.dtos.response;

import java.math.BigDecimal;
import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product information returned by the API")
public class ProductResponse {

    @Schema(description = "Unique product identifier", example = "1")
    private int productId;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones")
    private String productName;

    @Schema(description = "Product description", example = "High-quality wireless headphones")
    private String description;

    @Schema(description = "Product price", example = "49.99")
    private BigDecimal price;

    @Schema(description = "Category ID the product belongs to", example = "1")
    private int categoryId;

    @Schema(description = "Category name", example = "Electronics")
    private String categoryName;

    @Schema(description = "Product creation timestamp")
    private Timestamp createdAt;

    @Schema(description = "Total available stock", example = "25")
    private Integer stock;

//    // Manual setters for compatibility
//    public void setProductId(int productId) { this.productId = productId; }
//    public void setProductName(String productName) { this.productName = productName; }
//    public void setDescription(String description) { this.description = description; }
//    public void setPrice(BigDecimal price) { this.price = price; }
//    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
//    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
//    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public boolean getInStock() {
        return stock != null && stock > 0;
    };
}