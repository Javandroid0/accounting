package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderViewModel extends AndroidViewModel {
    private final OrderRepository orderRepository;
    private final MutableLiveData<OrderEntity> currentOrder;
    private final MutableLiveData<List<OrderItemEntity>> currentOrderItems;
    private final MutableLiveData<ProductEntity> lastScannedProduct = new MutableLiveData<>();
    private long currentUserId;

    public OrderViewModel(Application application) {
        super(application);
        orderRepository = new OrderRepository(application);

        // Create order with current date
        Date currentDate = new Date();
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(currentDate);

        currentOrder = new MutableLiveData<>(new OrderEntity(dateString, 0.0, 0L, currentUserId));
        currentOrderItems = new MutableLiveData<>();
        currentOrderItems.setValue(new ArrayList<>());
    }

    public void addProduct(ProductEntity product, double quantity) {
        List<OrderItemEntity> items = currentOrderItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        // Check if product already exists in the order
        boolean productExists = false;
        for (OrderItemEntity item : items) {
            if (item.getProductId() == product.getProductId()) {
                // Product already exists, update quantity instead of adding new item
                double newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity);

                // Update total in current order
                OrderEntity order = currentOrder.getValue();
                if (order != null) {
                    order.setTotal(order.getTotal() + (quantity * product.getSellPrice()));
                    currentOrder.setValue(order);
                }

                productExists = true;
                break;
            }
        }

        // If product doesn't exist, add new item
        if (!productExists) {
            OrderItemEntity orderItem = new OrderItemEntity(0, product.getBarcode());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setBuyPrice(product.getBuyPrice());
            orderItem.setSellPrice(product.getSellPrice());
            orderItem.setQuantity(quantity);

            // Update total in current order
            OrderEntity order = currentOrder.getValue();
            if (order != null) {
                order.setTotal(order.getTotal() + (quantity * product.getSellPrice()));
                currentOrder.setValue(order);
            }

            items.add(orderItem);
        }

        currentOrderItems.setValue(items);
    }

    public void removeItem(OrderItemEntity item) {
        List<OrderItemEntity> items = currentOrderItems.getValue();
        if (items != null && items.contains(item)) {
            // Update total in current order
            OrderEntity order = currentOrder.getValue();
            if (order != null) {
                order.setTotal(order.getTotal() - (item.getQuantity() * item.getSellPrice()));
                currentOrder.setValue(order);
            }

            items.remove(item);
            currentOrderItems.setValue(items);
        }
    }

    public void updateQuantity(OrderItemEntity item, double newQuantity) {
        List<OrderItemEntity> items = currentOrderItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
            currentOrderItems.setValue(items);
            return;
        }

        for (OrderItemEntity orderItem : items) {
            if (orderItem.getItemId() == item.getItemId()) {
                double priceDiff = (newQuantity - orderItem.getQuantity()) * orderItem.getSellPrice();
                orderItem.setQuantity(newQuantity);

                // Update total in current order
                OrderEntity order = currentOrder.getValue();
                if (order != null) {
                    order.setTotal(order.getTotal() + priceDiff);
                    currentOrder.setValue(order);
                }

                currentOrderItems.setValue(items);
                break;
            }
        }
    }

    public void confirmOrder() {
        OrderEntity order = currentOrder.getValue();
        List<OrderItemEntity> items = currentOrderItems.getValue();

        if (order != null && items != null && !items.isEmpty()) {
            orderRepository.insertOrderAndGetId(order, orderId -> {
                // Update order ID in items and insert them
                for (OrderItemEntity item : items) {
                    item.setOrderId(orderId);
                    orderRepository.insertOrderItem(item);
                }

                // Clear current order
                currentOrder.postValue(new OrderEntity(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 0.0, 0L, currentUserId));
                // Reset to empty list, not null
                currentOrderItems.postValue(new ArrayList<>());
            });
        }
    }

    public LiveData<OrderEntity> getCurrentOrder() {
        return currentOrder;
    }

    public LiveData<List<OrderItemEntity>> getCurrentOrderItems() {
        return currentOrderItems;
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        return orderRepository.getAllOrders();
    }

    public LiveData<List<OrderItemEntity>> getOrderItems(long orderId) {
        return orderRepository.getOrderItems(orderId);
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        return orderRepository.getOrdersByCustomerId(customerId);
    }

    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        return orderRepository.getOrdersByUserId(userId);
    }

    public LiveData<ProductEntity> getLastScannedProduct() {
        return lastScannedProduct;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
        OrderEntity order = currentOrder.getValue();
        if (order != null) {
            order.setUserId(userId);
            currentOrder.setValue(order);
        }
    }

    public void updateOrder(OrderEntity order) {
        orderRepository.updateOrder(order);
    }

    public void updateOrderItem(OrderItemEntity orderItem) {
        orderRepository.updateOrderItem(orderItem);
    }

    public void deleteOrder(OrderEntity order) {
        orderRepository.deleteOrder(order);
    }

    public void deleteOrderItem(OrderItemEntity orderItem) {
        orderRepository.deleteOrderItem(orderItem);
    }

    public void deleteAllOrders() {
        orderRepository.deleteAllOrders();
    }
}
