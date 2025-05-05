package com.javandroid.accounting_app.domain.manager;

import com.javandroid.accounting_app.data.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderManager {
    private List<Order> orders = new ArrayList<>();

    public void addProduct(Order newOrder) {
        for (Order order : orders) {
            if (order.getProductId() == newOrder.getProductId()) {
                order.setQuantity(order.getQuantity() + 1);
                return;
            }
        }
        orders.add(newOrder);
    }

    public void updateQuantity(int productId, double newQuantity) {
        for (Order order : orders) {
            if (order.getProductId() == productId) {
                order.setQuantity(newQuantity);
                return;
            }
        }
    }

    public void clear() {
        orders.clear();
    }

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public double calculateTotal() {
        double total = 0;
        for (Order order : orders) {
            total += order.getQuantity() * order.getProductSellPrice();
        }
        return total;
    }

    public void setOrders(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
    }

    public void deleteOrder(int productId) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getProductId() == productId) {
                orders.remove(i);
                return;
            }
        }
    }


}
