package com.javandroid.accounting_app.ui.viewmodel.new_order;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer; // Required for internal LiveData observation

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderItemRepository;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrentOrderViewModel extends AndroidViewModel {
    private static final String TAG = "CurrentOrderViewModel";

    private final OrderRepository orderRepository;
    public final OrderItemRepository orderItemRepository;
    private final OrderSessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Stable LiveData instances for Fragment observation
    private final MutableLiveData<OrderEntity> _fragmentOrderLiveData = new MutableLiveData<>();

    public LiveData<OrderEntity> getFragmentOrderLiveData() {
        return _fragmentOrderLiveData;
    }

    private final MutableLiveData<List<OrderItemEntity>> _fragmentOrderItemsLiveData = new MutableLiveData<>();

    public LiveData<List<OrderItemEntity>> getFragmentOrderItemsLiveData() {
        return _fragmentOrderItemsLiveData;
    }

    // Observers for current session's LiveData
    private Observer<OrderEntity> sessionOrderObserver;
    private LiveData<OrderEntity> currentSessionOrderLiveDataInternal;

    private Observer<List<OrderItemEntity>> sessionItemsObserver;
    private LiveData<List<OrderItemEntity>> currentSessionItemsLiveDataInternal;


    public CurrentOrderViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        orderItemRepository = new OrderItemRepository(application);
        sessionManager = OrderSessionManager.getInstance();
        // Initial setup of observers for the current session's data
        observeCurrentSessionData();
    }

    public void updateOrder(OrderEntity order) {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        if (order != null && currentRepo != null) {
            currentRepo.setCurrentOrder(order);
        }
    }

    private OrderStateRepository getCurrentStateRepository() {
        return sessionManager.getCurrentRepository();
    }

    /**
     * Sets up observers to bridge LiveData from the current OrderStateRepository
     * to the stable LiveData instances exposed to the Fragment.
     * This should be called initially and after every session reset.
     */
    private void observeCurrentSessionData() {
        OrderStateRepository currentRepo = getCurrentStateRepository();

        // Stop observing previous session's LiveData if any
        if (sessionOrderObserver != null && currentSessionOrderLiveDataInternal != null) {
            currentSessionOrderLiveDataInternal.removeObserver(sessionOrderObserver);
        }
        if (sessionItemsObserver != null && currentSessionItemsLiveDataInternal != null) {
            currentSessionItemsLiveDataInternal.removeObserver(sessionItemsObserver);
        }

        // Get LiveData from the NEW current session's repository
        currentSessionOrderLiveDataInternal = currentRepo.getCurrentOrder();
        currentSessionItemsLiveDataInternal = currentRepo.getCurrentOrderItems();


        // Create new observers
        sessionOrderObserver = orderEntity -> _fragmentOrderLiveData.setValue(orderEntity);
        sessionItemsObserver = orderItems -> _fragmentOrderItemsLiveData.setValue(orderItems);

        // Start observing the new session's LiveData
        // Use observeForever as this ViewModel outlives Fragments sometimes,
        // but ensure they are removed in onCleared.
        currentSessionOrderLiveDataInternal.observeForever(sessionOrderObserver);
        currentSessionItemsLiveDataInternal.observeForever(sessionItemsObserver);

        Log.d(TAG, "Now observing data from new/current OrderStateRepository.");
    }


    public void addProduct(ProductEntity product, double quantity) {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity order = currentRepo.getCurrentOrderValue(); // Get current order state
        List<OrderItemEntity> items = currentRepo.getCurrentOrderItemsValue();

        if (order == null) {
            Log.e(TAG, "Cannot add product, current order is null in repository.");
            // This case should ideally be prevented by OrderStateRepository always having an order.
            // If it happens, re-initialize or log error.
            // For now, let's ensure order is not null by re-fetching (though less ideal)
            // OrderStateRepository constructor ensures it's not null.
            return;
        }


        if (items == null) items = new ArrayList<>();
        List<OrderItemEntity> newItems = new ArrayList<>(items);

        boolean productExists = false;
        for (OrderItemEntity item : newItems) {
            if (item.getProductId() != null && item.getProductId().equals(product.getProductId())) {
                double newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity);
                productExists = true;
                break;
            }
        }

        if (!productExists) {
            // For new items to be inserted into DB, itemId should be 0 for Room to auto-generate.
            // The tempId from OrderStateRepository is for in-session UI tracking (e.g., DiffUtil uniqueness if needed there).
            // Here, we create the OrderItemEntity that will go into the list observed by the UI.
            long tempDisplayId = currentRepo.generateTempId(); // For UI list stability before saving

            OrderItemEntity orderItem = new OrderItemEntity(tempDisplayId, product.getBarcode());
            // When saving, this tempDisplayId will be converted to 0 if it's negative (or handled appropriately)
            // so DB generates a real ID.

            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setBuyPrice(product.getBuyPrice());
            orderItem.setSellPrice(product.getSellPrice());
            orderItem.setQuantity(quantity);
            newItems.add(orderItem);
        }

        // Update total in the current order object from the repository
        double newTotal = calculateTotalFromItems(newItems);
        order.setTotal(newTotal);

        // Post updates back to the OrderStateRepository's LiveData
        currentRepo.setCurrentOrder(order); // This updates the order object (including total)
        currentRepo.setCurrentOrderItems(newItems); // This updates the items list
    }

    public void removeItem(OrderItemEntity itemToRemove) {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity order = currentRepo.getCurrentOrderValue();
        List<OrderItemEntity> items = currentRepo.getCurrentOrderItemsValue();

        if (order == null || items == null) return;

        List<OrderItemEntity> newItems = new ArrayList<>(items);
        boolean removed = newItems.removeIf(item -> item.getItemId() == itemToRemove.getItemId());

        if (removed) {
            double newTotal = calculateTotalFromItems(newItems);
            order.setTotal(newTotal);
            currentRepo.setCurrentOrder(order);
            currentRepo.setCurrentOrderItems(newItems);
        }
    }

    public void updateQuantity(OrderItemEntity itemToUpdate, double newQuantity) {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity order = currentRepo.getCurrentOrderValue();
        List<OrderItemEntity> items = currentRepo.getCurrentOrderItemsValue();

        if (order == null || items == null) return;

        List<OrderItemEntity> newItems = new ArrayList<>(items); // Work with a copy
        boolean itemFound = false;
        for (OrderItemEntity orderItem : newItems) {
            if (orderItem.getItemId() == itemToUpdate.getItemId()) {
                orderItem.setQuantity(newQuantity);
                itemFound = true;
                break;
            }
        }

        if (itemFound) {
            double newTotal = calculateTotalFromItems(newItems);
            order.setTotal(newTotal);
            currentRepo.setCurrentOrder(order);
            currentRepo.setCurrentOrderItems(newItems);
        } else {
            Log.w(TAG, "Tried to update quantity for non-existent item: " + itemToUpdate.getProductName());
        }
    }

    private double calculateTotalFromItems(List<OrderItemEntity> items) {
        if (items == null) return 0.0;
        double total = 0.0;
        for (OrderItemEntity item : items) {
            total += item.getQuantity() * item.getSellPrice();
        }
        return Math.max(0.0, total); // Ensure non-negative
    }


    public void confirmOrder() {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity currentOrderData = currentRepo.getCurrentOrderValue();
        List<OrderItemEntity> currentItemsData = currentRepo.getCurrentOrderItemsValue();

        if (currentOrderData == null || currentItemsData == null || currentItemsData.isEmpty()) {
            Log.w(TAG, "Cannot confirm order: order or items are null/empty.");
            mainHandler.post(() -> Toast.makeText(getApplication(), "Cannot save empty order.", Toast.LENGTH_SHORT).show());
            return;
        }

        final long userId = currentOrderData.getUserId();
        final long customerId = currentOrderData.getCustomerId();
        final double calculatedTotal = calculateTotalFromItems(currentItemsData); // Use fresh total
        final boolean isPaid = currentOrderData.isPaid();

        Log.d(TAG, "Confirming order - Customer: " + customerId + ", User: " + userId +
                ", Calculated Total: " + calculatedTotal + ", Items: " + currentItemsData.size());

        if (customerId <= 0 || userId <= 0) {
            Log.e(TAG, "Cannot confirm order: invalid customer ID (" + customerId +
                    ") or user ID (" + userId + ")");
            mainHandler.post(() -> Toast.makeText(getApplication(), "Error: Missing customer or user for the order.", Toast.LENGTH_LONG).show());
            return;
        }

        // Create a fresh OrderEntity for insertion to avoid side effects with LiveData object
        OrderEntity orderToInsert = new OrderEntity(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                calculatedTotal,
                customerId,
                userId
        ); // orderId will be 0, DAO will generate
        orderToInsert.setPaid(isPaid);

        final List<OrderItemEntity> finalItemsToSaveInDb = new ArrayList<>();
        for (OrderItemEntity sessionItem : currentItemsData) {
            // Create fresh DB-bound items. Set itemId to 0 for Room to auto-generate.
            OrderItemEntity dbItem = new OrderItemEntity(0, sessionItem.getBarcode());
            dbItem.setProductId(sessionItem.getProductId());
            dbItem.setProductName(sessionItem.getProductName());
            dbItem.setBuyPrice(sessionItem.getBuyPrice());
            dbItem.setSellPrice(sessionItem.getSellPrice());
            dbItem.setQuantity(sessionItem.getQuantity());
            // orderId will be set after orderToInsert is saved
            finalItemsToSaveInDb.add(dbItem);
        }

        orderRepository.insertOrderAndGetId(orderToInsert, orderId -> {
            if (orderId > 0) {
                Log.d(TAG, "Order saved with DB ID: " + orderId + ". Saving items.");
                for (OrderItemEntity dbItemToSave : finalItemsToSaveInDb) {
                    dbItemToSave.setOrderId(orderId); // Link item to the new order ID
                    orderItemRepository.insertOrderItem(dbItemToSave);
                }
                Log.d(TAG, "All " + finalItemsToSaveInDb.size() + " items saved for order #" + orderId);
                mainHandler.post(() -> Toast.makeText(getApplication(), "Order #" + orderId + " saved successfully.", Toast.LENGTH_SHORT).show());

                mainHandler.post(() -> {
                    Log.d(TAG, "Order confirmed. Creating new order session for user: " + userId);
                    sessionManager.createNewSession(userId);
                    observeCurrentSessionData(); // Crucial: Re-bridge LiveData to the new session
                });
            } else {
                Log.e(TAG, "Failed to insert order into database, returned ID was " + orderId);
                mainHandler.post(() -> Toast.makeText(getApplication(), "Error saving order.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    public void confirmOrderAndThen(Runnable callback) {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity currentOrderData = currentRepo.getCurrentOrderValue();
        List<OrderItemEntity> currentItemsData = currentRepo.getCurrentOrderItemsValue();

        if (currentOrderData == null || currentItemsData == null || currentItemsData.isEmpty()) {
            Log.w(TAG, "confirmOrderAndThen: No order or items to confirm.");
            mainHandler.post(() -> Toast.makeText(getApplication(), "No items to confirm for printing.", Toast.LENGTH_SHORT).show());
            if (callback != null)
                mainHandler.post(callback); // Run callback if any, e.g., to close a dialog
            return;
        }

        final long userId = currentOrderData.getUserId();
        final long customerId = currentOrderData.getCustomerId();
        final double calculatedTotal = calculateTotalFromItems(currentItemsData);
        final boolean isPaid = currentOrderData.isPaid();

        Log.d(TAG, "Confirming order (then callback) - Customer: " + customerId + ", User: " + userId +
                ", Total: " + calculatedTotal + ", Items: " + currentItemsData.size());

        if (customerId <= 0 || userId <= 0) {
            Log.e(TAG, "confirmOrderAndThen: Invalid customer/user ID.");
            mainHandler.post(() -> Toast.makeText(getApplication(), "Error: Missing customer or user.", Toast.LENGTH_LONG).show());
            if (callback != null) mainHandler.post(callback);
            return;
        }

        OrderEntity orderToInsert = new OrderEntity(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                calculatedTotal, customerId, userId
        );
        orderToInsert.setPaid(isPaid);

        final List<OrderItemEntity> finalItemsToSaveInDb = new ArrayList<>();
        for (OrderItemEntity sessionItem : currentItemsData) {
            OrderItemEntity dbItem = new OrderItemEntity(0, sessionItem.getBarcode()); // itemId = 0 for insert
            dbItem.setProductId(sessionItem.getProductId());
            dbItem.setProductName(sessionItem.getProductName());
            dbItem.setBuyPrice(sessionItem.getBuyPrice());
            dbItem.setSellPrice(sessionItem.getSellPrice());
            dbItem.setQuantity(sessionItem.getQuantity());
            finalItemsToSaveInDb.add(dbItem);
        }

        orderRepository.insertOrderAndGetId(orderToInsert, orderId -> {
            if (orderId > 0) {
                Log.d(TAG, "Order (then callback) saved with ID: " + orderId);
                // Update the order object in the *current session* with the new ID for printing
                // This is okay because this session is about to be replaced.
                currentOrderData.setOrderId(orderId);
                currentOrderData.setTotal(calculatedTotal); // Ensure it has the final total for printing
                currentRepo.setCurrentOrder(currentOrderData); // Update for potential immediate use by callback

                for (OrderItemEntity dbItem : finalItemsToSaveInDb) {
                    dbItem.setOrderId(orderId);
                    orderItemRepository.insertOrderItem(dbItem);
                }

                if (callback != null) {
                    Log.d(TAG, "Executing post-confirmation callback.");
                    mainHandler.post(callback); // Ensure callback runs on main thread
                }

                mainHandler.post(() -> {
                    Log.d(TAG, "Order confirmed (then callback). Creating new session for user: " + userId);
                    sessionManager.createNewSession(userId);
                    observeCurrentSessionData(); // Re-bridge LiveData
                });
            } else {
                Log.e(TAG, "Failed to insert order (then callback), returned ID: " + orderId);
                mainHandler.post(() -> Toast.makeText(getApplication(), "Error saving order for printing.", Toast.LENGTH_SHORT).show());
                if (callback != null) mainHandler.post(callback); // Still run callback
            }
        });
    }


    public void updateOrderTotal() { // This can be called if external logic changes items directly
        OrderStateRepository currentRepo = getCurrentStateRepository();
        OrderEntity order = currentRepo.getCurrentOrderValue();
        List<OrderItemEntity> items = currentRepo.getCurrentOrderItemsValue();
        if (order != null) {
            double newTotal = calculateTotalFromItems(items);
            if (Math.abs(order.getTotal() - newTotal) > 0.001) {
                order.setTotal(newTotal);
                currentRepo.setCurrentOrder(order); // This will trigger LiveData update
            }
        }
    }

    // refreshItems might not be needed if LiveData from session is always the source of truth.
    // public void refreshItems(List<OrderItemEntity> items) { ... }


    public void resetCurrentOrderInternal() {
        OrderStateRepository currentRepo = getCurrentStateRepository();
        long userId = 0;
        OrderEntity currentOrder = currentRepo.getCurrentOrderValue();
        if (currentOrder != null) userId = currentOrder.getUserId();

        Log.d(TAG, "Resetting current order session. Creating new session for user ID: " + userId);
        sessionManager.createNewSession(userId);
        observeCurrentSessionData(); // Re-bridge LiveData to the new session
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove observers from the session's LiveData
        if (sessionOrderObserver != null && currentSessionOrderLiveDataInternal != null) {
            currentSessionOrderLiveDataInternal.removeObserver(sessionOrderObserver);
        }
        if (sessionItemsObserver != null && currentSessionItemsLiveDataInternal != null) {
            currentSessionItemsLiveDataInternal.removeObserver(sessionItemsObserver);
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        mainHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "CurrentOrderViewModel cleared.");
    }
}