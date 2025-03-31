package com.javandroid.accounting_app.model;

public class Order {
    private int orderId;
    private int userId;
    private int productId;
    private String productName;
    private double sellPrice;
    private double buyPrice;
    private int quantity;

    public Order(int orderId, int userId, int productId, String productName, double sellPrice, double buyPrice, int quantity) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.sellPrice = sellPrice;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
    }

    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getSellPrice() { return sellPrice; }
    public double getBuyPrice() { return buyPrice; }
    public int getQuantity() { return quantity; }

    public double calculateProfit() {
        return (sellPrice - buyPrice) * quantity;
    }
}
