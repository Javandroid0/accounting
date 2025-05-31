package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast; // For user feedback within ViewModel (consider moving to Fragment)

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity; // Needed for stock adjustment
import com.javandroid.accounting_app.data.repository.OrderItemRepository;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.ProductRepository; // To adjust stock
import com.javandroid.accounting_app.data.repository.OrderSessionManager;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderEditViewModel extends AndroidViewModel {
    private static final String TAG = "OrderEditViewModel";

    private final OrderRepository orderRepository;
    public final OrderItemRepository orderItemRepository; // Made public final in previous updates
    private final ProductRepository productRepository;   // For stock updates
    private final OrderSessionManager sessionManager;    // For shared state logic if any remains
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public OrderEditViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        orderItemRepository = new OrderItemRepository(application);
        productRepository = new ProductRepository(application); // Initialize ProductRepository
        sessionManager = OrderSessionManager.getInstance();
    }

    private OrderStateRepository getStateRepository() { // For any shared state operations
        return sessionManager.getCurrentRepository();
    }

    public void setEditingOrder(OrderEntity order) {
        Log.d(TAG, "Setting order for editing (shared state): ID=" + (order != null ? order.getOrderId() : "null"));
        OrderStateRepository currentSharedRepo = getStateRepository();
        if (order == null) {
            currentSharedRepo.setCurrentOrder(null);
            currentSharedRepo.setCurrentOrderItems(new ArrayList<>());
            return;
        }

        currentSharedRepo.setCurrentOrder(order);

        if (order.getOrderId() > 0) {
            LiveData<List<OrderItemEntity>> itemsLiveData = orderItemRepository.getOrderItems(order.getOrderId());
            itemsLiveData.observeForever(new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                @Override
                public void onChanged(List<OrderItemEntity> orderItems) {
                    if (orderItems != null) {
                        Log.d(TAG, "Loaded " + orderItems.size() + " items for order ID=" + order.getOrderId() + " into shared state");
                        currentSharedRepo.setCurrentOrderItems(orderItems);
                    } else {
                        currentSharedRepo.setCurrentOrderItems(new ArrayList<>());
                    }
                    itemsLiveData.removeObserver(this);
                }
            });
        } else {
            currentSharedRepo.setCurrentOrderItems(new ArrayList<>());
        }
    }

    public void saveModifiedOrderAndItems(
            OrderEntity editedOrder,
            List<OrderItemEntity> currentItemsInEdit,
            List<OrderItemEntity> originalItemsFromDb, // Snapshot of items before edit began
            Runnable onComplete) {

        if (editedOrder == null || editedOrder.getOrderId() <= 0) {
            Log.e(TAG, "Cannot save: Invalid OrderEntity or orderId.");
            mainThreadHandler.post(() -> {
                Toast.makeText(getApplication(), "Error: Invalid order data.", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run(); // Signal completion even on error
            });
            return;
        }

        Log.d(TAG, "Saving modified order ID: " + editedOrder.getOrderId() + ". Edited items: " + currentItemsInEdit.size() + ", Original DB items: " + originalItemsFromDb.size());

        executor.execute(() -> {
            try {
                // --- 1. Update the Order Header ---
                orderRepository.updateOrder(editedOrder);
                Log.d(TAG, "Order header updated for ID: " + editedOrder.getOrderId());

                // --- 2. Calculate Stock Adjustments ---
                // Map<ProductId, NetQuantityChange>
                Map<Long, Double> productStockAdjustments = new HashMap<>();

                // Process original items to see what was returned to stock or initially taken
                for (OrderItemEntity originalItem : originalItemsFromDb) {
                    if (originalItem.getProductId() != null && originalItem.getProductId() > 0) {
                        // Quantity from original item effectively "returned to stock" before new quantities are "taken"
                        productStockAdjustments.put(originalItem.getProductId(),
                                productStockAdjustments.getOrDefault(originalItem.getProductId(), 0.0) + originalItem.getQuantity());
                    }
                }
                // Process edited items to see what is being "taken from stock"
                for (OrderItemEntity editedItem : currentItemsInEdit) {
                    if (editedItem.getProductId() != null && editedItem.getProductId() > 0) {
                        // Quantity from edited item is "taken from stock"
                        productStockAdjustments.put(editedItem.getProductId(),
                                productStockAdjustments.getOrDefault(editedItem.getProductId(), 0.0) - editedItem.getQuantity());
                    }
                }

                // --- 3. Reconcile Order Items in DB ---
                List<OrderItemEntity> itemsToDeleteFromDb = new ArrayList<>(originalItemsFromDb);

                for (OrderItemEntity editedItem : currentItemsInEdit) {
                    editedItem.setOrderId(editedOrder.getOrderId());
                    boolean foundInOriginalDb = false;
                    for (int i = 0; i < itemsToDeleteFromDb.size(); i++) {
                        if (itemsToDeleteFromDb.get(i).getItemId() == editedItem.getItemId()) {
                            orderItemRepository.updateOrderItem(editedItem);
                            Log.d(TAG, "Updated item ID: " + editedItem.getItemId() + " (" + editedItem.getProductName() + ")");
                            itemsToDeleteFromDb.remove(i);
                            foundInOriginalDb = true;
                            break;
                        }
                    }
                    if (!foundInOriginalDb) { // New item added during this edit session
                        // Ensure itemId is 0 or less for the DAO to auto-generate a new primary key
                        if (editedItem.getItemId() > 0) { // If it had a positive ID but wasn't in original, it's new to THIS order context
                            editedItem.setItemId(0); // Force insert
                        }
                        orderItemRepository.insertOrderItem(editedItem);
                        Log.d(TAG, "Inserted new item: " + editedItem.getProductName() + " for order " + editedOrder.getOrderId());
                    }
                }
                for (OrderItemEntity itemToRemove : itemsToDeleteFromDb) {
                    orderItemRepository.deleteOrderItem(itemToRemove);
                    Log.d(TAG, "Deleted item ID: " + itemToRemove.getItemId() + " (" + itemToRemove.getProductName() + ") from order " + editedOrder.getOrderId());
                }
                Log.d(TAG, "Order items reconciled for order ID: " + editedOrder.getOrderId());

                // --- 4. Apply Stock Adjustments ---
                for (Map.Entry<Long, Double> entry : productStockAdjustments.entrySet()) {
                    Long productId = entry.getKey();
                    Double netStockChange = entry.getValue(); // Positive means stock increases (items returned > items taken)
                    // Negative means stock decreases (items taken > items returned)
                    ProductEntity product = productRepository.getProductByIdSync(productId); // Fetch current product
                    if (product != null) {
                        Log.d(TAG, "Stock for " + product.getName() + " (ID:" + productId + "): current=" + product.getStock() + ", net change required=" + (-netStockChange));
                        product.setStock(product.getStock() - netStockChange); // If netStockChange is +ve (returned), stock increases. If -ve (taken), stock decreases.
                        productRepository.update(product);
                        Log.d(TAG, "Product ID " + productId + " stock updated to: " + product.getStock());
                    } else {
                        Log.w(TAG, "Product ID " + productId + " not found for stock adjustment.");
                    }
                }
                Log.d(TAG, "Product stock adjustments applied for order ID: " + editedOrder.getOrderId());

                if (onComplete != null) {
                    mainThreadHandler.post(onComplete);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving modified order and items for ID: " + editedOrder.getOrderId(), e);
                mainThreadHandler.post(() -> {
                    Toast.makeText(getApplication(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (onComplete != null) onComplete.run(); // Notify completion even on error
                });
            }
        });
    }


    public void cancelOrderEditing() {
        OrderStateRepository currentSharedRepo = getStateRepository();
        OrderEntity currentOrderValue = currentSharedRepo.getCurrentOrderValue();
        long userIdForNewSession = (currentOrderValue != null) ? currentOrderValue.getUserId() : 0;
        Log.d(TAG, "Canceling edits in shared state. Resetting shared session for user ID: " + userIdForNewSession);
        sessionManager.createNewSession(userIdForNewSession);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}