package com.javandroid.accounting_app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton repository to coordinate shared state between ViewModels
 */
public class OrderStateRepository {
    // Singleton instance
    private static OrderStateRepository instance;

    // Shared state that multiple ViewModels need access to
    private final MutableLiveData<OrderEntity> currentOrder = new MutableLiveData<>();
    private final MutableLiveData<List<OrderItemEntity>> currentOrderItems = new MutableLiveData<>();
    private long currentUserId;
    private long currentCustomerId;

    // Store in-progress orders and items for each customer
    private final Map<Long, OrderEntity> customerOrders = new HashMap<>();
    private final Map<Long, List<OrderItemEntity>> customerOrderItems = new HashMap<>();

    // Counter for generating temporary unique IDs
    private final AtomicLong tempIdCounter = new AtomicLong(Integer.MAX_VALUE / 2);

    // Private constructor to enforce singleton
    private OrderStateRepository() {
        // Initialize with an empty order
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        currentOrder.setValue(new OrderEntity(dateString, 0.0, 0L, 0L));
        currentOrderItems.setValue(new ArrayList<>());
    }

    // Get singleton instance
    public static synchronized OrderStateRepository getInstance() {
        if (instance == null) {
            instance = new OrderStateRepository();
        }
        return instance;
    }

    // Current order methods
    public LiveData<OrderEntity> getCurrentOrder() {
        return currentOrder;
    }

    public void setCurrentOrder(OrderEntity order) {
        currentOrder.setValue(order);
    }

    public OrderEntity getCurrentOrderValue() {
        return currentOrder.getValue();
    }

    // Current order items methods
    public LiveData<List<OrderItemEntity>> getCurrentOrderItems() {
        return currentOrderItems;
    }

    public void setCurrentOrderItems(List<OrderItemEntity> items) {
        currentOrderItems.setValue(new ArrayList<>(items));
    }

    public List<OrderItemEntity> getCurrentOrderItemsValue() {
        return currentOrderItems.getValue();
    }

    // User ID methods
    public long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
        OrderEntity order = currentOrder.getValue();
        if (order != null) {
            order.setUserId(userId);
            currentOrder.setValue(order);
        }
    }

    // Customer ID methods
    public long getCurrentCustomerId() {
        return currentCustomerId;
    }

    public void setCurrentCustomerId(long customerId) {
        this.currentCustomerId = customerId;
        OrderEntity order = currentOrder.getValue();
        if (order != null) {
            order.setCustomerId(customerId);
            currentOrder.setValue(order);
        }
    }

    // Customer order state methods
    public void storeOrderStateForCustomer(long customerId, OrderEntity order, List<OrderItemEntity> items) {
        if (order != null) {
            OrderEntity clone = cloneOrder(order);
            customerOrders.put(customerId, clone);
        }

        if (items != null) {
            List<OrderItemEntity> itemsCopy = new ArrayList<>();
            for (OrderItemEntity item : items) {
                OrderItemEntity copy = cloneOrderItem(item);
                itemsCopy.add(copy);
            }
            customerOrderItems.put(customerId, itemsCopy);
        }
    }

    public OrderEntity getStoredOrderForCustomer(long customerId) {
        return customerOrders.get(customerId);
    }

    public List<OrderItemEntity> getStoredOrderItemsForCustomer(long customerId) {
        return customerOrderItems.get(customerId);
    }

    public boolean hasStoredOrderForCustomer(long customerId) {
        return customerOrders.containsKey(customerId);
    }

    public void clearStoredOrderForCustomer(long customerId) {
        customerOrders.remove(customerId);
        customerOrderItems.remove(customerId);
    }

    public void clearAllStoredOrders() {
        customerOrders.clear();
        customerOrderItems.clear();
    }

    // Temp ID generation
    public long generateTempId() {
        return tempIdCounter.getAndIncrement();
    }

    // Helper methods
    private OrderEntity cloneOrder(OrderEntity order) {
        OrderEntity clone = new OrderEntity(
                order.getDate(),
                order.getTotal(),
                order.getCustomerId(),
                order.getUserId());
        clone.setOrderId(order.getOrderId());
        return clone;
    }

    private OrderItemEntity cloneOrderItem(OrderItemEntity item) {
        OrderItemEntity copy = new OrderItemEntity(item.getItemId(), item.getBarcode());
        copy.setOrderId(item.getOrderId());
        copy.setProductId(item.getProductId());
        copy.setProductName(item.getProductName());
        copy.setBuyPrice(item.getBuyPrice());
        copy.setSellPrice(item.getSellPrice());
        copy.setQuantity(item.getQuantity());
        return copy;
    }
}