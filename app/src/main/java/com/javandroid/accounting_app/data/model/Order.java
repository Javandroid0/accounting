package com.javandroid.accounting_app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders")
public class Order {
    @PrimaryKey(autoGenerate = true)
    private int orderId;
    private String userId;

    private String productId; // barcode
    private String productName;
    private double productSellPrice;
    private double productBuyPrice;
    private int quantity;



    // Getters & Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductSellPrice() { return productSellPrice; }
    public void setProductSellPrice(double productSellPrice) { this.productSellPrice = productSellPrice; }

    public double getProductBuyPrice() { return productBuyPrice; }
    public void setProductBuyPrice(double productBuyPrice) { this.productBuyPrice = productBuyPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
