package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.time.LocalDateTime;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;

    private String currentUserId;
    private final MutableLiveData<List<Order>> currentOrders = new MutableLiveData<>(new ArrayList<>());

    public OrderViewModel(Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        setCurrentUserId(); // Initialize a user session
    }

    // LiveData for observing order list for current user
    public LiveData<List<Order>> getCurrentOrderList() {
        return currentOrders;
    }

    // Load from DB if needed
    public void loadOrdersForCurrentUser() {
        orderRepository.getOrdersByUserId(currentUserId).observeForever(orders -> {
            currentOrders.setValue(new ArrayList<>(orders)); // Copy to allow mutation
        });
    }

    public void addProductToOrder(Order order) {
        orderRepository.insert(order);
        Log.d("OrderViewModel", "Product added: " + order.getProductName());

        List<Order> updatedOrders = new ArrayList<>(currentOrders.getValue());
        updatedOrders.add(order);
        currentOrders.setValue(updatedOrders);
    }

    public void removeProductFromOrder(Order order) {
        orderRepository.delete(order);
        Log.d("OrderViewModel", "Product removed: " + order.getProductName());

        List<Order> updatedOrders = new ArrayList<>(currentOrders.getValue());
        updatedOrders.removeIf(o -> o.getOrderId() == order.getOrderId());
        currentOrders.setValue(updatedOrders);
    }

    public void updateOrder(Order updatedOrder) {
        orderRepository.update(updatedOrder);
        Log.d("OrderViewModel", "Product updated: " + updatedOrder.getProductName());

        List<Order> updatedOrders = new ArrayList<>(currentOrders.getValue());
        for (int i = 0; i < updatedOrders.size(); i++) {
            if (updatedOrders.get(i).getOrderId() == updatedOrder.getOrderId()) {
                updatedOrders.set(i, updatedOrder);
                break;
            }
        }
        currentOrders.setValue(updatedOrders);
    }

    public double calculateTotal(List<Order> orders) {
        double total = 0.0;
        for (Order order : orders) {
            total += order.getQuantity() * order.getProductSellPrice();
        }
        return total;
    }

    public LiveData<Double> getUserDebt() {
        return Transformations.map(currentOrders, orders -> {
            double total = 0;
            for (Order order : orders) {
                total += order.getQuantity() * order.getProductSellPrice();
            }
            return total;
        });
    }

    public void confirmOrderForUser(String userId) {
        // You can log or show that orders for this user are considered confirmed
        Log.d("OrderViewModel", "Confirmed orders for user: " + userId);

        // Generate a new unique user ID (just +1 for simplicity)
//        currentUserId = userId + 1;
        setCurrentUserId();

        Log.d("OrderViewModel", "Next order session for user: " + currentUserId);
    }

    public String getCurrentUserId() {
        return this.currentUserId;
    }

    public void setCurrentUserId() {
        this.currentUserId = LocalDateTime.now().toString();
    }
}
