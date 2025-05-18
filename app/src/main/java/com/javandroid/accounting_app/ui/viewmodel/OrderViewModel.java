package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.ProductRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class OrderViewModel extends AndroidViewModel {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MutableLiveData<OrderEntity> currentOrder;
    private final MutableLiveData<List<OrderItemEntity>> currentOrderItems;
    private long currentUserId;
    private static final String TAG = "OrderViewModel";

    // Store in-progress orders and items for each customer
    private final Map<Long, OrderEntity> customerOrders = new HashMap<>();
    private final Map<Long, List<OrderItemEntity>> customerOrderItems = new HashMap<>();

    // Event to signal when order is empty/deleted
    private final MutableLiveData<Boolean> orderEmptyEvent = new MutableLiveData<>();

    // New: Event to signal when product is not found
    private final MutableLiveData<String> productNotFoundEvent = new MutableLiveData<>();

    // Counter for generating temporary unique IDs - using a positive sequence for
    // better compatibility
    private final AtomicLong tempIdCounter = new AtomicLong(Integer.MAX_VALUE / 2);

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // New: Event types for product operations
    public enum ProductOperationResult {
        ADDED_SUCCESSFULLY,
        OUT_OF_STOCK,
        NOT_FOUND,
        ERROR
    }

    // New: Message class for product operations
    public static class ProductOperationMessage {
        private final ProductOperationResult result;
        private final String message;
        private final ProductEntity product;

        public ProductOperationMessage(ProductOperationResult result, String message, ProductEntity product) {
            this.result = result;
            this.message = message;
            this.product = product;
        }

        public ProductOperationResult getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }

        public ProductEntity getProduct() {
            return product;
        }
    }

    // New event for product operation results
    private final MutableLiveData<ProductOperationMessage> productOperationMessage = new MutableLiveData<>();

    // New: Event to handle product operations
    public LiveData<ProductOperationMessage> getProductOperationMessage() {
        return productOperationMessage;
    }

    public OrderViewModel(Application application) {
        super(application);
        orderRepository = createRepository(application);
        productRepository = new ProductRepository(application);

        // Create order with current date
        Date currentDate = new Date();
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(currentDate);

        currentOrder = new MutableLiveData<>(new OrderEntity(dateString, 0.0, 0L, currentUserId));
        currentOrderItems = new MutableLiveData<>();
        currentOrderItems.setValue(new ArrayList<>());
    }

    /**
     * Create the repository - can be overridden in tests for mocking
     */
    protected OrderRepository createRepository(Application application) {
        return new OrderRepository(application);
    }

    // Updated method that combines getLastScannedProduct and addProduct
    // functionality
    public void addProductByBarcode(String barcode, double quantity) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                // Get product by barcode
                ProductEntity product = productRepository.getProductByBarcodeSync(barcode);

                if (product != null) {
                    // Check if there's enough stock
                    if (product.getStock() >= quantity) {
                        // Product found with stock, add to order on main thread
                        new Handler(getApplication().getMainLooper()).post(() -> {
                            addProduct(product, quantity);

                            // Notify UI of success
                            productOperationMessage.setValue(
                                    new ProductOperationMessage(
                                            ProductOperationResult.ADDED_SUCCESSFULLY,
                                            "Product added: " + product.getName(),
                                            product));
                        });
                    } else {
                        // Not enough stock
                        productOperationMessage.postValue(
                                new ProductOperationMessage(
                                        ProductOperationResult.OUT_OF_STOCK,
                                        "Product out of stock: " + product.getName(),
                                        product));
                    }
                } else {
                    // Product not found, trigger event for UI to handle
                    productNotFoundEvent.postValue(barcode);
                    productOperationMessage.postValue(
                            new ProductOperationMessage(
                                    ProductOperationResult.NOT_FOUND,
                                    "Product not found for barcode: " + barcode,
                                    null));
                }
            } catch (Exception e) {
                // Handle errors
                productOperationMessage.postValue(
                        new ProductOperationMessage(
                                ProductOperationResult.ERROR,
                                "Error processing barcode: " + e.getMessage(),
                                null));
            }
        });
    }

    // New: Event to handle when product is not found
    public LiveData<String> getProductNotFoundEvent() {
        return productNotFoundEvent;
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
            OrderEntity currentOrderValue = currentOrder.getValue();
            boolean isEditMode = currentOrderValue != null && currentOrderValue.getOrderId() > 0;

            // First check if this is a saved item and we're not in edit mode
            if (!isEditMode && item.getOrderId() != null && item.getOrderId() > 0) {
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

                                // Clean up observers to prevent repeated loading
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    cleanupOrderObservers(orderId);
                                });
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
                double itemTotal = item.getQuantity() * item.getSellPrice();
                double newTotal = order.getTotal() - itemTotal;

                // Ensure total is never negative
                if (newTotal < 0) {
                    newTotal = 0.0;
                }

                order.setTotal(newTotal);
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

            // If this was a saved order and we're not in edit mode, refresh the data
            if (!isEditMode && item.getOrderId() != null && item.getOrderId() > 0) {
                // Give database a moment to update then refresh
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    long orderId = item.getOrderId();

                    // Create observer that can be removed after use
                    final androidx.lifecycle.Observer<List<OrderItemEntity>> observer = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                        @Override
                        public void onChanged(List<OrderItemEntity> refreshedItems) {
                            if (refreshedItems != null) {
                                if (refreshedItems.isEmpty()) {
                                    // Signal that the order is now empty/deleted
                                    orderEmptyEvent.postValue(true);
                                    cleanupOrderObservers(orderId);
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
                // Calculate the price difference based on the quantity change
                double priceDiff = (newQuantity - orderItem.getQuantity()) * orderItem.getSellPrice();
                orderItem.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for item " + orderItem.getProductName() +
                        " (ID=" + orderItem.getItemId() + ") from " +
                        item.getQuantity() + " to " + newQuantity);

                // IMPORTANT: No longer update database immediately for existing items
                // Changes will only be saved when the order is confirmed
                // if (orderItem.getOrderId() != null && orderItem.getOrderId() > 0) {
                // Log.d(TAG, "Saving changes to database for order item ID: " +
                // orderItem.getItemId());
                // updateOrderItem(orderItem);
                // }

                // Update total in current order
                OrderEntity order = currentOrder.getValue();
                if (order != null) {
                    // Ensure order total is never negative by using the correct approach for
                    // calculation
                    double newTotal = order.getTotal() + priceDiff;
                    if (newTotal < 0) {
                        // For orders that shouldn't have negative totals, set to zero or a minimum
                        // value
                        // or handle the negative case specifically if it represents a refund or credit
                        Log.w(TAG, "Order total would be negative (" + newTotal + "). Adjusting calculation.");

                        // Option 1: Recalculate the total based on all items to ensure correctness
                        updateOrderTotal();
                    } else {
                        order.setTotal(newTotal);
                        currentOrder.setValue(order);
                    }
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
            // Get current customer ID to clear state later
            final long customerId = order.getCustomerId();

            orderRepository.insertOrderAndGetId(order, orderId -> {
                // Save any pending changes to existing items first
                for (OrderItemEntity item : items) {
                    // Update existing items in the database
                    if (item.getOrderId() != null && item.getOrderId() > 0) {
                        orderRepository.updateOrderItem(item);
                    } else {
                        // Set order ID for new items
                        item.setOrderId(orderId);
                        orderRepository.insertOrderItem(item);
                    }
                }

                // Clear current order
                currentOrder.postValue(new OrderEntity(
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 0.0,
                        customerId,
                        currentUserId));
                // Reset to empty list, not null
                currentOrderItems.postValue(new ArrayList<>());

                // Clear this customer's saved order state since it's now confirmed
                customerOrders.remove(customerId);
                customerOrderItems.remove(customerId);

                // Also clear any other customer's cached orders to prevent state leakage
                customerOrders.clear();
                customerOrderItems.clear();
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
            // Get current customer ID to clear state later
            final long customerId = order.getCustomerId();

            orderRepository.insertOrderAndGetId(order, orderId -> {
                Log.d(TAG, "Order confirmed with ID: " + orderId);

                // Set the order ID in the current order (for printing)
                order.setOrderId(orderId);
                currentOrder.postValue(order);

                // Save any pending changes to existing items first
                for (OrderItemEntity item : items) {
                    // Update existing items in the database
                    if (item.getOrderId() != null && item.getOrderId() > 0) {
                        orderRepository.updateOrderItem(item);
                    } else {
                        // Set order ID for new items
                        item.setOrderId(orderId);
                        orderRepository.insertOrderItem(item);
                    }
                }

                // Execute the callback (e.g., for printing)
                if (callback != null) {
                    callback.run();
                }

                // After the callback is done, create a new order
                currentOrder.postValue(new OrderEntity(
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 0.0,
                        customerId,
                        currentUserId));
                // Reset to empty list, not null
                currentOrderItems.postValue(new ArrayList<>());

                // Clear this customer's saved order state since it's now confirmed
                customerOrders.remove(customerId);
                customerOrderItems.remove(customerId);

                // Also clear any other customer's cached orders to prevent state leakage
                customerOrders.clear();
                customerOrderItems.clear();
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

        // Automatically load the order items for this order
        if (order.getOrderId() > 0) {
            // Use a one-time observer to load the order items
            final androidx.lifecycle.Observer<List<OrderItemEntity>> observer = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                @Override
                public void onChanged(List<OrderItemEntity> orderItems) {
                    if (orderItems != null) {
                        Log.d(TAG, "Loaded " + orderItems.size() + " items for order ID=" + order.getOrderId());
                        replaceCurrentOrderItems(orderItems);
                    } else {
                        Log.w(TAG, "No items found for order ID=" + order.getOrderId());
                        // Set empty list instead of null
                        replaceCurrentOrderItems(new ArrayList<>());
                    }
                    // Remove observer after use to prevent memory leaks
                    getOrderItems(order.getOrderId()).removeObserver(this);
                }
            };

            // Start observing
            getOrderItems(order.getOrderId()).observeForever(observer);
        }
    }

    /**
     * Replace the current order items list with items from a saved order
     */
    public void replaceCurrentOrderItems(List<OrderItemEntity> items) {
        // Create a new list to ensure proper diffing

        List<OrderItemEntity> newItems = new ArrayList<>(items);

        currentOrderItems.setValue(newItems);
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        return orderRepository.getOrdersByCustomerId(customerId);
    }

    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        return orderRepository.getOrdersByUserId(userId);
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
        OrderEntity order = currentOrder.getValue();
        if (order != null) {
            order.setUserId(userId);
            currentOrder.setValue(order);
        }
    }

    /**
     * Set the customer ID for the current order and switch to that customer's order
     * list
     */
    public void setCustomerId(long customerId) {
        OrderEntity order = currentOrder.getValue();
        if (order != null) {
            // Save current order state for the previous customer
            if (order.getCustomerId() > 0) {
                Log.d(TAG, "DEBUG: Saving state for previous customer: " + order.getCustomerId() +
                        " before switching to customer: " + customerId);
                saveCurrentOrderStateForCustomer(order.getCustomerId());
            }

            // Set new customer ID
            order.setCustomerId(customerId);

            // If we already have an order for this customer, load it
            if (customerOrders.containsKey(customerId)) {
                OrderEntity savedOrder = customerOrders.get(customerId);
                Log.d(TAG, "DEBUG: Restoring cached order for customer: " + customerId +
                        ", cached total: " + (savedOrder != null ? savedOrder.getTotal() : "null"));

                // Update with saved data but keep the same order instance to avoid
                // breaking references in the UI
                assert savedOrder != null;
                order.setTotal(savedOrder.getTotal());

                // Load this customer's items
                List<OrderItemEntity> savedItems = customerOrderItems.get(customerId);
                if (savedItems != null) {
                    Log.d(TAG, "DEBUG: Restoring " + savedItems.size() + " cached items for customer: " + customerId);
                    currentOrderItems.setValue(new ArrayList<>(savedItems));
                } else {
                    Log.d(TAG, "DEBUG: No cached items for customer: " + customerId + ", using empty list");
                    currentOrderItems.setValue(new ArrayList<>());
                }
            } else {
                // First time seeing this customer, start with empty order
                Log.d(TAG, "DEBUG: First visit for customer: " + customerId + ", starting with empty order");
                order.setTotal(0.0);
                currentOrderItems.setValue(new ArrayList<>());
            }

            currentOrder.setValue(order);
        } else {
            Log.w(TAG, "DEBUG: Cannot set customer ID, current order is null");
        }
    }

    /**
     * Save the current order state for a customer
     */
    private void saveCurrentOrderStateForCustomer(long customerId) {
        OrderEntity currentOrderValue = currentOrder.getValue();
        List<OrderItemEntity> currentItemsValue = currentOrderItems.getValue();

        if (currentOrderValue != null) {
            Log.d(TAG, "DEBUG: Caching order for customer: " + customerId +
                    ", total: " + currentOrderValue.getTotal());
            customerOrders.put(customerId, cloneOrder(currentOrderValue));
        }

        if (currentItemsValue != null) {
            List<OrderItemEntity> itemsCopy = new ArrayList<>();
            for (OrderItemEntity item : currentItemsValue) {
                // Create a deep copy of each item
                OrderItemEntity copy = new OrderItemEntity(item.getItemId(), item.getBarcode());
                copy.setOrderId(item.getOrderId());
                copy.setProductId(item.getProductId());
                copy.setProductName(item.getProductName());
                copy.setBuyPrice(item.getBuyPrice());
                copy.setSellPrice(item.getSellPrice());
                copy.setQuantity(item.getQuantity());
                itemsCopy.add(copy);
            }
            Log.d(TAG, "DEBUG: Caching " + itemsCopy.size() + " items for customer: " + customerId);
            customerOrderItems.put(customerId, itemsCopy);
        } else {
            Log.d(TAG, "DEBUG: No items to cache for customer: " + customerId);
        }
    }

    /**
     * Create a copy of an order
     */
    private OrderEntity cloneOrder(OrderEntity order) {
        OrderEntity clone = new OrderEntity(
                order.getDate(),
                order.getTotal(),
                order.getCustomerId(),
                order.getUserId());
        clone.setOrderId(order.getOrderId());
        return clone;
    }

    /**
     * Recalculates the total for the current order based on all order items
     */
    public void updateOrderTotal() {
        OrderEntity order = currentOrder.getValue();
        List<OrderItemEntity> items = currentOrderItems.getValue();

        if (order != null && items != null) {
            double total = 0.0;

            // Sum up the price of all items (quantity * sellPrice)
            for (OrderItemEntity item : items) {
                total += item.getQuantity() * item.getSellPrice();
            }

            // Ensure total is never negative - for business purposes
            // If your business allows negative totals (refunds, etc.) remove this check
            if (total < 0) {
                Log.w(TAG, "Calculated negative total: " + total + ". Setting to 0.");
                total = 0.0;
            }

            // Update the order total
            order.setTotal(total);
            currentOrder.setValue(order);
        }
    }

    /**
     * Remove all observers for a specific order ID
     */
    public void cleanupOrderObservers(long orderId) {
        Log.d(TAG, "DEBUG: Starting cleanup of observers for order ID: " + orderId);

        // Get the LiveData for this order's items
        LiveData<List<OrderItemEntity>> orderItemsLiveData = getOrderItems(orderId);

        // Force remove all observers to prevent leaks and repeated loading
        if (orderItemsLiveData instanceof MutableLiveData) {
            // Clear internal observers - this is a way to force cleanup
            try {
                Field observersField = LiveData.class.getDeclaredField("mObservers");
                observersField.setAccessible(true);
                Object observers = observersField.get(orderItemsLiveData);
                Method methodClear = observers.getClass().getDeclaredMethod("clear");
                methodClear.setAccessible(true);
                methodClear.invoke(observers);
                Log.d(TAG, "DEBUG: Successfully cleaned up observers for order ID: " + orderId);
            } catch (Exception e) {
                Log.e(TAG, "DEBUG: Failed to clean up observers: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "DEBUG: Could not clean observers for order ID: " + orderId +
                    " - LiveData is not MutableLiveData");
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

    public void resetCurrentOrder() {
        // Create a new empty order with current date
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        OrderEntity newOrder = new OrderEntity(dateString, 0.0, 0L, currentUserId);

        // Explicitly set orderId to 0 to ensure it's treated as a new order
        newOrder.setOrderId(0);

        Log.d(TAG, "DEBUG: Resetting current order to new empty order, userId: " + currentUserId);

        // Set the new order
        currentOrder.setValue(newOrder);

        // Reset order items to a completely new empty list
        List<OrderItemEntity> oldItems = currentOrderItems.getValue();
        int oldItemCount = oldItems != null ? oldItems.size() : 0;
        currentOrderItems.setValue(new ArrayList<>());

        Log.d(TAG, "DEBUG: Current order has been fully reset, cleared " + oldItemCount + " items");
    }

    // Improved method to cancel order editing
    public void cancelOrderEditing() {
        OrderEntity currentOrderValue = currentOrder.getValue();
        if (currentOrderValue != null && currentOrderValue.getOrderId() > 0) {
            // This is an existing order, reload it from database to discard changes
            long orderId = currentOrderValue.getOrderId();
            Log.d(TAG, "DEBUG: Canceling edits for order ID: " + orderId + ", customerId: " +
                    currentOrderValue.getCustomerId() + ", items: " +
                    (currentOrderItems.getValue() != null ? currentOrderItems.getValue().size() : 0));

            // Use a one-time observer to load the original order
            final androidx.lifecycle.Observer<OrderEntity> orderObserver = new androidx.lifecycle.Observer<OrderEntity>() {
                @Override
                public void onChanged(OrderEntity order) {
                    if (order != null) {
                        Log.d(TAG, "DEBUG: Reloaded order from DB: " + order.getOrderId() +
                                ", total: " + order.getTotal());
                        currentOrder.setValue(order);
                        // Remove observer after use to prevent memory leaks
                        getOrderById(orderId).removeObserver(this);

                        // Also reload items
                        final androidx.lifecycle.Observer<List<OrderItemEntity>> itemsObserver = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                            @Override
                            public void onChanged(List<OrderItemEntity> items) {
                                if (items != null) {
                                    Log.d(TAG, "DEBUG: Reloaded " + items.size() + " items for order ID: " + orderId);
                                    replaceCurrentOrderItems(items);
                                } else {
                                    Log.d(TAG, "DEBUG: No items loaded for order ID: " + orderId);
                                }
                                // Remove observer after use
                                getOrderItems(orderId).removeObserver(this);
                            }
                        };

                        // Start observing items
                        getOrderItems(orderId).observeForever(itemsObserver);
                    } else {
                        Log.w(TAG, "DEBUG: Failed to reload order: " + orderId + " (returned null)");
                    }
                }
            };

            // Start observing order
            getOrderById(orderId).observeForever(orderObserver);
        } else {
            // This is a new order, just reset it
            Log.d(TAG, "DEBUG: Canceling edits for NEW order (orderId=0), customer: " +
                    (currentOrderValue != null ? currentOrderValue.getCustomerId() : "null") +
                    ", items: " + (currentOrderItems.getValue() != null ? currentOrderItems.getValue().size() : 0));
            resetCurrentOrder();
        }

        // Log current state of caches before clearing
        Log.d(TAG, "DEBUG: Current customer order cache size: " + customerOrders.size() +
                ", keys: " + customerOrders.keySet());
        Log.d(TAG, "DEBUG: Current customer items cache size: " + customerOrderItems.size() +
                ", keys: " + customerOrderItems.keySet());

        // IMPORTANT: Clear ALL cached order data regardless of customer ID
        // This ensures no order data leaks between operations
        customerOrders.clear();
        customerOrderItems.clear();

        Log.d(TAG, "DEBUG: All customer caches cleared");

        // Force garbage collection to clear any lingering references
        System.gc();
    }

    /**
     * Explicitly save all changes for an existing order
     * Use this when saving changes in OrderDetailsFragment
     */
    public void saveOrderChanges() {
        OrderEntity order = currentOrder.getValue();
        List<OrderItemEntity> items = currentOrderItems.getValue();

        if (order != null && order.getOrderId() > 0 && items != null) {
            Log.d(TAG, "DEBUG: Saving changes for order ID: " + order.getOrderId() +
                    ", customer: " + order.getCustomerId() +
                    ", items: " + items.size() +
                    ", total: " + order.getTotal());

            // First update the order itself
            updateOrder(order);

            // Log each item being saved
            for (OrderItemEntity item : items) {
                if (item.getOrderId() != null && item.getOrderId() > 0) {
                    // Update existing items
                    Log.d(TAG, "DEBUG: Updating existing item: " + item.getItemId() +
                            ", orderId: " + item.getOrderId() +
                            ", product: " + item.getProductName() +
                            ", quantity: " + item.getQuantity() +
                            ", price: " + item.getSellPrice());
                    updateOrderItem(item);
                } else if (item.getOrderId() != null) {
                    // This is a new item for an existing order
                    Log.d(TAG, "DEBUG: Adding new item to existing order: " +
                            "tempId: " + item.getItemId() +
                            ", product: " + item.getProductName() +
                            ", quantity: " + item.getQuantity() +
                            ", price: " + item.getSellPrice());
                    item.setOrderId(order.getOrderId());
                    orderRepository.insertOrderItem(item);
                }
            }

            // Recalculate and save the total
            updateOrderTotal();
            updateOrder(order);

            Log.d(TAG, "DEBUG: Saved all changes for order ID: " + order.getOrderId() +
                    " with " + items.size() + " items, final total: " + order.getTotal());

            // Log current state of caches
            Log.d(TAG, "DEBUG: Customer order cache size: " + customerOrders.size() +
                    ", keys: " + customerOrders.keySet());
            Log.d(TAG, "DEBUG: Customer items cache size: " + customerOrderItems.size() +
                    ", keys: " + customerOrderItems.keySet());
        } else {
            Log.w(TAG, "DEBUG: Attempted to save changes without a valid order, " +
                    "orderNull: " + (order == null) +
                    ", orderId: " + (order != null ? order.getOrderId() : "null") +
                    ", itemsNull: " + (items == null) +
                    ", itemsSize: " + (items != null ? items.size() : 0));
        }
    }

    /**
     * Delete an order and all its items from the database immediately
     * Use this when canceling an order in OrderDetailsFragment
     */
    public void deleteOrderAndItems(long orderId) {
        Log.d(TAG, "DEBUG: Deleting order ID: " + orderId + " and all its items from database");

        // First, get the order from database
        executor.execute(() -> {
            try {
                // Get the order entity from the database
                OrderEntity orderToDelete = orderRepository.getOrderByIdSync(orderId);
                if (orderToDelete != null) {
                    // Delete the order from database
                    orderRepository.deleteOrder(orderToDelete);
                    Log.d(TAG, "DEBUG: Order deleted from database: " + orderId);

                    // Reset the current order to a new empty order
                    new Handler(Looper.getMainLooper()).post(() -> resetCurrentOrder());
                } else {
                    Log.w(TAG, "DEBUG: Could not find order to delete: " + orderId);
                }

                // Clean up observers to prevent repeated loading
                new Handler(Looper.getMainLooper()).post(() -> cleanupOrderObservers(orderId));
            } catch (Exception e) {
                Log.e(TAG, "ERROR: Failed to delete order: " + e.getMessage(), e);
            }
        });
    }
}
