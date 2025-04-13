package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.repository.OrderRepository;

import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final MutableLiveData<List<Order>> orderListLiveData = new MutableLiveData<>();

    public OrderViewModel(Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
    }

    // Method to fetch the current list of orders
    public LiveData<List<Order>> getOrders() {
//        orderRepository.getAllOrders(orderListLiveData);
        orderRepository.getAllOrders();
        return orderListLiveData;
    }

    // Method to add a product to the current order (pass in the product)
    public void addProductToOrder(Order order) {
        orderRepository.insert(order);
        Log.d("OrderViewModel", "Product added to order: " + order.getProductName());
    }

    // Method to remove a product from the order
    public void removeProductFromOrder(Order order) {
        orderRepository.delete(order);
        Log.d("OrderViewModel", "Product removed from order: " + order.getProductName());
    }

    // Method to calculate the total amount of the current order
    public double calculateTotal(List<Order> orders) {
        double totalAmount = 0.0;
        for (Order order : orders) {
            totalAmount += order.getQuantity() * order.getProductSellPrice();
        }
        return totalAmount;
    }

    // Method to confirm the current order (could add to a confirmed orders table or something similar)
    public void confirmOrder() {
        Log.d("OrderViewModel", "Order confirmed");
        // Add confirmation logic here
    }
}
