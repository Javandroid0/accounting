package com.javandroid.accounting_app.model;

public class Order {
    private final int orderId;
    private final int userId;
    private final int productId;
    private final String type;
    private final String productName;
    private final double sellPrice;
    private final double buyPrice;
    private final int quantity;

    public Order(int orderId, int userId, String type, int productId, String productName, double sellPrice, double buyPrice, int quantity) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.type = type;
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

    public String getType() {
        return type;
    }
}
