package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.domain.manager.OrderManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {
    private final OrderRepository orderRepository;
    private final OrderManager orderManager = new OrderManager();

    private final MutableLiveData<List<Order>> currentOrders = new MutableLiveData<>();
    private String currentUserId;

    public OrderViewModel(Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        setCurrentUserId();
        currentOrders.setValue(orderManager.getOrders());
    }

    public void addOrUpdateProduct(Order order) {
        orderManager.addProduct(order);
        currentOrders.setValue(orderManager.getOrders());
    }

    public void updateQuantity(int productId, double newQuantity) {
        orderManager.updateQuantity(productId, newQuantity);
        currentOrders.setValue(orderManager.getOrders());
    }

    public void confirmOrder() {
        for (Order order : orderManager.getOrders()) {
            orderRepository.insert(order);
        }
        orderManager.clear();
        setCurrentUserId();
        currentOrders.setValue(orderManager.getOrders());
    }

    public LiveData<List<Order>> getCurrentOrders() {
        return currentOrders;
    }
    public Product getProductByBarcode(String barcode) {
        // You can search for the product in your repository or database
        return orderRepository.getProductByBarcode(barcode);
    }

    public double calculateTotal() {
        return orderManager.calculateTotal();
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId() {
        currentUserId = LocalDateTime.now().toString();
    }

    // Delete a single order from the list
    public void deleteOrder(Order order) {
        List<Order> orders = new ArrayList<>(orderManager.getOrders());
        orders.remove(order);
        orderManager.setOrders(orders);  // You need to implement setOrders in OrderManager if not existing yet
        currentOrders.setValue(orderManager.getOrders());
    }

    // Update the whole list of orders
    public void updateOrders(List<Order> updatedOrders) {
        orderManager.setOrders(updatedOrders);
        currentOrders.setValue(orderManager.getOrders());
    }
}
