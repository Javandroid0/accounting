package com.javandroid.accounting_app;

import com.javandroid.accounting_app.model.Product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private String type; // "Sale" or "Purchase"
    private List<OrderItem> items;
    private long orderDate; // Store as Unix timestamp (milliseconds)

    public Order() {
        this.items = new ArrayList<>();
        this.orderDate = System.currentTimeMillis(); // Set current time on creation
    }

    public Order(String orderId, String userId, String type) {
        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.items = new ArrayList<>();
        this.orderDate = System.currentTimeMillis();
    }

    public Order(String orderId, String userId, String type, long orderDate) {
        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.items = new ArrayList<>();
        this.orderDate = orderDate;
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public long getOrderDate() {
        return orderDate;
    }

    // Setters
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void setOrderDate(long orderDate) {
        this.orderDate = orderDate;
    }

    public void addItem(Product product, int quantity) {
        OrderItem newItem = new OrderItem(
                product.getId(),
                product.getName(),
                product.getSellPrice(),
                product.getBuyPrice(),
                quantity
        );
        this.items.add(newItem);
    }

    public double getTotalCost() {
        double totalCost = 0;
        for (OrderItem item : items) {
            totalCost += item.getItemTotalCost();
        }
        return totalCost;
    }

    public double getTotalSellPrice() {
        double totalSellPrice = 0;
        for (OrderItem item : items) {
            totalSellPrice += item.getItemTotalSellPrice();
        }
        return totalSellPrice;
    }

    public double getTotalProfit() {
        return getTotalSellPrice() - getTotalCost();
    }

    // Helper method to get the order date as a Date object
    public Date getOrderDateAsDate() {
        return new Date(orderDate);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", type='" + type + '\'' +
                ", items=" + items +
                ", orderDate=" + getOrderDateAsDate() +
                '}';
    }
}