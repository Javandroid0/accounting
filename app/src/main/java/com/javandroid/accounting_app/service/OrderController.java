package com.javandroid.accounting_app.service;


import com.javandroid.accounting_app.model.Order;

import java.util.List;
import java.util.stream.Collectors;

public class OrderController {
    private ExcelManager excelManager;

    public OrderController(ExcelManager excelManager) {
        this.excelManager = excelManager;
    }

    public List<Order> getAllOrders() {
        return excelManager.getAllOrders();
    }

    public void addOrder(Order order) {
        excelManager.addOrder(order);
    }

    public double calculateTotalCost(List<Order> orders) {
        return orders.stream()
                .mapToDouble(order -> order.getBuyPrice() * order.getQuantity())
                .sum();
    }

    public double calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
                .mapToDouble(order -> order.getSellPrice() * order.getQuantity())
                .sum();
    }

    public double calculateTotalProfit(List<Order> orders) {
        return calculateTotalRevenue(orders) - calculateTotalCost(orders);
    }

    public List<Order> getOrdersByUser(int userId) {
        return getAllOrders().stream()
                .filter(order -> order.getUserId() == userId)
                .collect(Collectors.toList());
    }
}

