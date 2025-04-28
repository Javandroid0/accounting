package com.javandroid.accounting_app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders")
public class Order {
    @PrimaryKey(autoGenerate = true)
    private int orderId;
    private String userId;

    private int productId;

    private String productBarcode;
    private String productName;
    private double productSellPrice;
    private double productBuyPrice;
    private double quantity;

    public Order(){}

    public Order(String currentUserId, int id, String name,String productBarcode, int i, double sellPrice,double productBuyPrice) {
        this.userId = currentUserId;
        this.productId = id;
        this.productName = name;
        this.productBarcode = productBarcode;
        this.quantity = i;
        this.productSellPrice = sellPrice;
        this.productBuyPrice = productBuyPrice;
    }


    // Getters & Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductSellPrice() { return productSellPrice; }
    public void setProductSellPrice(double productSellPrice) { this.productSellPrice = productSellPrice; }

    public double getProductBuyPrice() { return productBuyPrice; }
    public void setProductBuyPrice(double productBuyPrice) { this.productBuyPrice = productBuyPrice; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getProductBarcode() {
        return productBarcode;
    }

    public void setProductBarcode(String productBarcode) {
        this.productBarcode = productBarcode;
    }
}
