package com.javandroid.accounting_app.data.repository;

import android.os.Looper;
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
 * Repository to manage order state for a single order session
 * Each order session should have its own repository instance to prevent state
 * leakage
 */
public class OrderStateRepository {
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

    /**
     * Constructor initializes with an empty order
     * 
     * @param userId The current user ID, if known (0 if not yet set)
     */
    public OrderStateRepository(long userId) {
        // Initialize with an empty order
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        OrderEntity emptyOrder = new OrderEntity(dateString, 0.0, 0L, userId);
        emptyOrder.setOrderId(0); // Ensure it's a new order

        this.currentUserId = userId;
        currentOrder.setValue(emptyOrder);
        currentOrderItems.setValue(new ArrayList<>());
    }

    /**
     * Default constructor without user ID
     */
    public OrderStateRepository() {
        this(0);
    }

    // Current order methods
    public LiveData<OrderEntity> getCurrentOrder() {
        return currentOrder;
    }

    /**
     * Sets the current order - thread-safe method that handles both main and
     * background threads
     */
    public void setCurrentOrder(OrderEntity order) {
        if (isOnMainThread()) {
            currentOrder.setValue(order);
        } else {
            currentOrder.postValue(order);
        }
    }

    public OrderEntity getCurrentOrderValue() {
        return currentOrder.getValue();
    }

    // Current order items methods
    public LiveData<List<OrderItemEntity>> getCurrentOrderItems() {
        return currentOrderItems;
    }

    /**
     * Sets the current order items - thread-safe method that handles both main and
     * background threads
     */
    public void setCurrentOrderItems(List<OrderItemEntity> items) {
        List<OrderItemEntity> itemsCopy = new ArrayList<>(items);
        if (isOnMainThread()) {
            currentOrderItems.setValue(itemsCopy);
        } else {
            currentOrderItems.postValue(itemsCopy);
        }
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
            setCurrentOrder(order);
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
            setCurrentOrder(order);
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

    /**
     * Reset this repository to a fresh state with the given user ID
     * 
     * @param userId The user ID to associate with the new order
     */
    public void reset(long userId) {
        // Create a new empty order
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        OrderEntity emptyOrder = new OrderEntity(dateString, 0.0, 0L, userId);
        emptyOrder.setOrderId(0);

        // Clear and reset state
        this.currentUserId = userId;
        this.currentCustomerId = 0;
        customerOrders.clear();
        customerOrderItems.clear();

        // Update LiveData values
        setCurrentOrder(emptyOrder);
        setCurrentOrderItems(new ArrayList<>());
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

    /**
     * Checks if the current thread is the main thread
     */
    private boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}