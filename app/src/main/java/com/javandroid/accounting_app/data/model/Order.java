package com.javandroid.accounting_app.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "order1")
public class Order {
    @PrimaryKey(autoGenerate = true)
    private int orderId;

    @ColumnInfo(name = "user_id")
    private int userId;


    @ColumnInfo(name = "product_id")
    private String productId;

    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "product_sell_price")
    private double productSellPrice;

    @ColumnInfo(name = "product_buy_price")
    private double productBuyPrice;

    @ColumnInfo(name = "quantity")
    private int quantity;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getProductSellPrice() {
        return productSellPrice;
    }

    public void setProductSellPrice(double productSellPrice) {
        this.productSellPrice = productSellPrice;
    }

    public double getProductBuyPrice() {
        return productBuyPrice;
    }

    public void setProductBuyPrice(double productBuyPrice) {
        this.productBuyPrice = productBuyPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Getters and Setters
}
