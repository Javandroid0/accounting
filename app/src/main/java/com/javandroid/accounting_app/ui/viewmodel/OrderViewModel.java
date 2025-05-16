package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderViewModel extends AndroidViewModel {
    private final OrderRepository orderRepository;
    private final MutableLiveData<OrderEntity> currentOrder;
    private final MutableLiveData<List<OrderItemEntity>> currentOrderItems;
    private final MutableLiveData<ProductEntity> lastScannedProduct = new MutableLiveData<>();
    private long currentUserId;
    private static final String TAG = "OrderViewModel";

    // Event to signal when order is empty/deleted
    private final MutableLiveData<Boolean> orderEmptyEvent = new MutableLiveData<>();

    // Counter for generating temporary unique IDs - using a positive sequence for
    // better compatibility
    private final AtomicLong tempIdCounter = new AtomicLong(Integer.MAX_VALUE / 2);

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        // Create a new list to trigger observer updates
        List<OrderItemEntity> newItems = new ArrayList<>(items);

        // Check if product already exists in the order
        boolean productExists = false;
        for (OrderItemEntity item : newItems) {
            if (item.getProductId() != null &&
                    item.getProductId() == product.getProductId()) {
                // Product already exists, update quantity instead of adding new item
                double newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for product " + product.getName() +
                        " (ID=" + product.getProductId() + ") from " +
                        (newQuantity - quantity) + " to " + newQuantity);

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
            // Generate a temporary unique ID (using a reserved high range to avoid
            // conflicts with DB IDs)
            long tempId = tempIdCounter.getAndIncrement();

            OrderItemEntity orderItem = new OrderItemEntity(tempId, product.getBarcode());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setBuyPrice(product.getBuyPrice());
            orderItem.setSellPrice(product.getSellPrice());
            orderItem.setQuantity(quantity);

            Log.d(TAG, "Added new product " + product.getName() +
                    " (ID=" + product.getProductId() + ") with temp itemId=" +
                    tempId + " and quantity=" + quantity);

            // Update total in current order
            OrderEntity order = currentOrder.getValue();
            if (order != null) {
                order.setTotal(order.getTotal() + (quantity * product.getSellPrice()));
                currentOrder.setValue(order);
            }

            newItems.add(orderItem);
        }

        // Set the new list to trigger observers
        currentOrderItems.setValue(newItems);
    }

    public void removeItem(OrderItemEntity item) {
        List<OrderItemEntity> items = currentOrderItems.getValue();
        if (items != null) {
            // First check if this is a saved item
            if (item.getOrderId() != null && item.getOrderId() > 0) {
                final Long orderId = item.getOrderId();
                final boolean isLastItem = countItemsForOrder(items, orderId) <= 1;

                // Execute delete operation on a background thread
                executor.execute(() -> {
                    try {
                        // Delete the item from database
                        orderRepository.deleteOrderItem(item);

                        // If this was the last item, delete the order too
                        if (isLastItem) {
                            Log.d(TAG, "Deleting order " + orderId + " as it has no more items");
                            // Get the order entity from the database
                            OrderEntity orderToDelete = orderRepository.getOrderByIdSync(orderId);
                            if (orderToDelete != null) {
                                orderRepository.deleteOrder(orderToDelete);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting item: " + e.getMessage());
                    }
                });
            }

            // Update total in current order
            OrderEntity order = currentOrder.getValue();
            if (order != null) {
                order.setTotal(order.getTotal() - (item.getQuantity() * item.getSellPrice()));
                currentOrder.setValue(order);
            }

            // Update UI by removing from list and notifying
            boolean removed = false;

            // First try direct object removal
            if (items.remove(item)) {
                removed = true;
            } else {
                // If direct object removal fails, try finding by ID
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).getItemId() == item.getItemId()) {
                        items.remove(i);
                        removed = true;
                        break;
                    }
                }
            }

            // Force the list to be considered as new to trigger UI updates
            List<OrderItemEntity> newList = new ArrayList<>(items);
            currentOrderItems.setValue(newList);

            // If this was a saved order, refresh the data
            if (item.getOrderId() != null && item.getOrderId() > 0) {
                // Give database a moment to update then refresh
                new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    long orderId = item.getOrderId();

                    // Create observer that can be removed after use
                    final androidx.lifecycle.Observer<List<OrderItemEntity>> observer = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                        @Override
                        public void onChanged(List<OrderItemEntity> refreshedItems) {
                            if (refreshedItems != null) {
                                if (refreshedItems.isEmpty()) {
                                    // Signal that the order is now empty/deleted
                                    orderEmptyEvent.postValue(true);
                                } else {
                                    replaceCurrentOrderItems(refreshedItems);
                                }
                            }

                            // Remove observer after use to prevent memory leaks
                            getOrderItems(orderId).removeObserver(this);
                        }
                    };

                    // Observe for a single update
                    getOrderItems(orderId).observeForever(observer);
                }, 100); // Short delay to ensure database update has time to complete
            }
        }
    }

    /**
     * Count items that belong to a specific order
     */
    private int countItemsForOrder(List<OrderItemEntity> items, long orderId) {
        int count = 0;
        for (OrderItemEntity item : items) {
            if (item.getOrderId() != null && item.getOrderId() == orderId) {
                count++;
            }
        }
        return count;
    }

    public void updateQuantity(OrderItemEntity item, double newQuantity) {
        List<OrderItemEntity> items = currentOrderItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
            currentOrderItems.setValue(items);
            return;
        }

        boolean itemFound = false;
        for (OrderItemEntity orderItem : items) {
            if (orderItem.getItemId() == item.getItemId()) {
                double priceDiff = (newQuantity - orderItem.getQuantity()) * orderItem.getSellPrice();
                orderItem.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for item " + orderItem.getProductName() +
                        " (ID=" + orderItem.getItemId() + ") from " +
                        item.getQuantity() + " to " + newQuantity);

                // If this is a saved item (has an orderId), update it in the database
                if (orderItem.getOrderId() != null && orderItem.getOrderId() > 0) {
                    Log.d(TAG, "Saving changes to database for order item ID: " + orderItem.getItemId());
                    updateOrderItem(orderItem);
                }

                // Update total in current order
                OrderEntity order = currentOrder.getValue();
                if (order != null) {
                    order.setTotal(order.getTotal() + priceDiff);
                    currentOrder.setValue(order);
                }

                itemFound = true;
                break;
            }
        }

        if (itemFound) {
            // Force the list to be considered as new to trigger UI updates
            List<OrderItemEntity> newList = new ArrayList<>(items);
            currentOrderItems.setValue(newList);
        } else {
            Log.w(TAG, "Tried to update quantity for non-existent item: " + item.getProductName() +
                    " (ID=" + item.getItemId() + ")");
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
                currentOrder.postValue(new OrderEntity(
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 0.0, 0L,
                        currentUserId));
                // Reset to empty list, not null
                currentOrderItems.postValue(new ArrayList<>());
            });
        }
    }

    /**
     * Confirms the order and executes a callback after the order has been saved
     * 
     * @param callback The callback to execute after the order is confirmed
     */
    public void confirmOrderAndThen(Runnable callback) {
        OrderEntity order = currentOrder.getValue();
        List<OrderItemEntity> items = currentOrderItems.getValue();

        if (order != null && items != null && !items.isEmpty()) {
            orderRepository.insertOrderAndGetId(order, orderId -> {
                Log.d(TAG, "Order confirmed with ID: " + orderId);

                // Set the order ID in the current order (for printing)
                order.setOrderId(orderId);
                currentOrder.postValue(order);

                // Update order ID in items and insert them
                for (OrderItemEntity item : items) {
                    item.setOrderId(orderId);
                    orderRepository.insertOrderItem(item);
                }

                // Execute the callback (e.g., for printing)
                if (callback != null) {
                    callback.run();
                }

                // After the callback is done, create a new order
                currentOrder.postValue(new OrderEntity(
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 0.0, 0L,
                        currentUserId));
                // Reset to empty list, not null
                currentOrderItems.postValue(new ArrayList<>());
            });
        } else {
            // If there's no valid order, just run the callback
            if (callback != null) {
                callback.run();
            }
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

    public LiveData<OrderEntity> getOrderById(long orderId) {
        return orderRepository.getOrderById(orderId);
    }

    /**
     * Set an existing order for editing
     */
    public void setEditingOrder(OrderEntity order) {
        Log.d(TAG, "Setting order for editing: ID=" + order.getOrderId());
        currentOrder.setValue(order);
        // The current order items will be set separately with replaceCurrentOrderItems
    }

    /**
     * Replace the current order items list with items from a saved order
     */
    public void replaceCurrentOrderItems(List<OrderItemEntity> items) {
        // Create a new list to ensure proper diffing
        List<OrderItemEntity> newItems = new ArrayList<>();

        for (OrderItemEntity item : items) {
            newItems.add(item);
        }

        currentOrderItems.setValue(newItems);
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

    public LiveData<Boolean> getOrderEmptyEvent() {
        return orderEmptyEvent;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the executor when ViewModel is cleared
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
