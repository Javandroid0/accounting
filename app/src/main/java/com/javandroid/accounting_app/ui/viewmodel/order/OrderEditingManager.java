package com.javandroid.accounting_app.ui.viewmodel.order; // Or your chosen package

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// import androidx.lifecycle.ViewModel; // Optional: if you want lifecycle awareness

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity; // If adding new products

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderEditingManager { // Can extend ViewModel if desired

    private final MutableLiveData<OrderEntity> editableOrderLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<OrderItemEntity>> editableOrderItemsLiveData = new MutableLiveData<>();

    private OrderEntity originalOrderSnapshot;
    private List<OrderItemEntity> originalItemsSnapshot;

    /**
     * Loads an order and its items for editing.
     * Creates deep copies to ensure the original data is not mutated directly,
     * and stores snapshots of the original data.
     *
     * @param orderToEdit The original order entity from the database.
     * @param itemsToEdit The original list of order items from the database.
     */
    public void loadOrderForEditing(OrderEntity orderToEdit, List<OrderItemEntity> itemsToEdit) {
        if (orderToEdit == null) {
            // Or handle more gracefully, e.g., clear LiveData or post an error state
            this.originalOrderSnapshot = null;
            this.originalItemsSnapshot = new ArrayList<>();
            this.editableOrderLiveData.setValue(null);
            this.editableOrderItemsLiveData.setValue(new ArrayList<>());
            return;
        }
        // Store deep copies as snapshots
        this.originalOrderSnapshot = deepCopyOrder(orderToEdit);
        this.originalItemsSnapshot = deepCopyItemsList(itemsToEdit);

        // Set LiveData with deep copies for editing
        this.editableOrderLiveData.setValue(deepCopyOrder(orderToEdit));
        this.editableOrderItemsLiveData.setValue(deepCopyItemsList(itemsToEdit));
    }

    // --- Getters for Snapshots ---

    /**
     * Returns a deep copy of the original order that was loaded for editing.
     * This ensures the snapshot itself isn't accidentally modified.
     *
     * @return A copy of the original OrderEntity snapshot.
     */
    public OrderEntity getOriginalOrderSnapshot() {
        return deepCopyOrder(originalOrderSnapshot);
    }

    /**
     * Returns a deep copy of the list of original order items that were loaded for editing.
     * This ensures the snapshot list itself isn't accidentally modified.
     *
     * @return A copy of the original list of OrderItemEntity snapshots.
     */
    public List<OrderItemEntity> getOriginalItemsSnapshot() {
        return deepCopyItemsList(originalItemsSnapshot);
    }


    // --- LiveData Getters for UI Observation ---
    public LiveData<OrderEntity> getLiveEditableOrder() {
        return editableOrderLiveData;
    }

    public LiveData<List<OrderItemEntity>> getLiveEditableOrderItems() {
        return editableOrderItemsLiveData;
    }

    // --- Direct Getters for Current Editable State (e.g., for saving) ---
    public OrderEntity getCurrentEditableOrder() {
        return editableOrderLiveData.getValue();
    }

    public List<OrderItemEntity> getCurrentEditableOrderItems() {
        return editableOrderItemsLiveData.getValue();
    }

    // --- Methods to Modify Editable State ---
    public void updateItemQuantity(long itemId, double newQuantity) {
        List<OrderItemEntity> currentItems = editableOrderItemsLiveData.getValue();
        if (currentItems != null) {
            List<OrderItemEntity> updatedItems = new ArrayList<>(currentItems); // Work on a copy
            boolean found = false;
            for (OrderItemEntity item : updatedItems) {
                if (item.getItemId() == itemId) {
                    item.setQuantity(newQuantity);
                    found = true;
                    break;
                }
            }
            if (found) {
                editableOrderItemsLiveData.setValue(updatedItems);
                recalculateTotal();
            }
        }
    }

    public void removeItem(long itemId) {
        List<OrderItemEntity> currentItems = editableOrderItemsLiveData.getValue();
        if (currentItems != null) {
            List<OrderItemEntity> updatedItems = new ArrayList<>(currentItems);
            boolean removed = updatedItems.removeIf(item -> item.getItemId() == itemId);
            if (removed) {
                editableOrderItemsLiveData.setValue(updatedItems);
                recalculateTotal();
            }
        }
    }

    // Example: if adding items during edit is supported
    public void addNewItem(ProductEntity product, double quantity) {
        OrderEntity currentOrder = editableOrderLiveData.getValue();
        if (currentOrder == null) return; // Cannot add item if no order context

        List<OrderItemEntity> currentItems = editableOrderItemsLiveData.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }
        List<OrderItemEntity> updatedItems = new ArrayList<>(currentItems);

        // For new items, itemId could be 0 or a temporary negative value
        // The actual database ID will be assigned upon saving.
        OrderItemEntity newItem = new OrderItemEntity(0, product.getBarcode()); // Temporary ID
        newItem.setOrderId(currentOrder.getOrderId()); // Associate with the currently edited order's ID
        newItem.setProductId(product.getProductId());
        newItem.setProductName(product.getName());
        newItem.setBuyPrice(product.getBuyPrice());
        newItem.setSellPrice(product.getSellPrice());
        newItem.setQuantity(quantity);

        updatedItems.add(newItem);
        editableOrderItemsLiveData.setValue(updatedItems);
        recalculateTotal();
    }


    private void recalculateTotal() {
        OrderEntity currentOrder = editableOrderLiveData.getValue();
        List<OrderItemEntity> currentItems = editableOrderItemsLiveData.getValue();

        if (currentOrder != null && currentItems != null) {
            double newCalculatedTotal = 0;
            for (OrderItemEntity item : currentItems) {
                newCalculatedTotal += item.getSellPrice() * item.getQuantity();
            }

            // Only update if the total actually changed to avoid unnecessary LiveData triggers
            if (Math.abs(currentOrder.getTotal() - newCalculatedTotal) > 0.001) {
                OrderEntity updatedOrder = deepCopyOrder(currentOrder); // Modify a copy
                updatedOrder.setTotal(newCalculatedTotal);
                editableOrderLiveData.setValue(updatedOrder);
            }
        }
    }

    public boolean hasChanges() {
        OrderEntity currentOrder = editableOrderLiveData.getValue();
        List<OrderItemEntity> currentItems = editableOrderItemsLiveData.getValue();

        // Case 1: Original state was not loaded or current state is null
        if (originalOrderSnapshot == null || currentOrder == null || originalItemsSnapshot == null || currentItems == null) {
            return !Objects.equals(originalOrderSnapshot, currentOrder) || !Objects.equals(originalItemsSnapshot, currentItems);
        }

        // Case 2: Compare order totals (simplistic check for header changes, as items affect total)
        // A more robust OrderEntity comparison might be needed if other header fields are editable.
        // Recalculate total of original items for accurate comparison if only items could change the total.
        double originalCalculatedTotal = 0;
        for (OrderItemEntity item : originalItemsSnapshot) {
            originalCalculatedTotal += item.getSellPrice() * item.getQuantity();
        }
        if (Math.abs(originalCalculatedTotal - currentOrder.getTotal()) > 0.001) return true;


        // Case 3: Compare item list sizes
        if (originalItemsSnapshot.size() != currentItems.size()) {
            return true;
        }

        // Case 4: Compare individual items
        // This assumes items are identifiable by itemId. New items (if itemId is 0 or negative) need special handling.
        for (OrderItemEntity originalItem : originalItemsSnapshot) {
            OrderItemEntity currentMatchingItem = null;
            for (OrderItemEntity ci : currentItems) {
                if (ci.getItemId() == originalItem.getItemId() && ci.getItemId() != 0) { // Match existing items
                    currentMatchingItem = ci;
                    break;
                }
            }

            if (currentMatchingItem == null) {
                return true; // Original item deleted
            }

            if (Math.abs(originalItem.getQuantity() - currentMatchingItem.getQuantity()) > 0.001 ||
                    Math.abs(originalItem.getSellPrice() - currentMatchingItem.getSellPrice()) > 0.001 || // If price is editable
                    !Objects.equals(originalItem.getProductName(), currentMatchingItem.getProductName())) { // If name is editable
                return true; // Item modified
            }
        }
        // Case 5: Check for newly added items (those in currentItems but not in originalItemsSnapshot by ID, or with temp ID)
        for (OrderItemEntity currentItem : currentItems) {
            boolean foundInOriginal = false;
            if (currentItem.getItemId() <= 0) { // Clearly a new item not from DB originally
                return true;
            }
            for (OrderItemEntity originalItem : originalItemsSnapshot) {
                if (originalItem.getItemId() == currentItem.getItemId()) {
                    foundInOriginal = true;
                    break;
                }
            }
            if (!foundInOriginal) {
                return true; // Item present in current list but wasn't in original (e.g. ID mismatch or truly new)
            }
        }


        return false;
    }

    // --- Deep Copy Helper Methods (essential for isolating state) ---
    private OrderEntity deepCopyOrder(OrderEntity original) {
        if (original == null) return null;
        OrderEntity copy = new OrderEntity(original.getDate(), original.getTotal(), original.getCustomerId(), original.getUserId());
        copy.setOrderId(original.getOrderId());
        return copy;
    }

    private List<OrderItemEntity> deepCopyItemsList(List<OrderItemEntity> originals) {
        if (originals == null) return new ArrayList<>(); // Return empty list, not null
        List<OrderItemEntity> copies = new ArrayList<>();
        for (OrderItemEntity original : originals) {
            copies.add(deepCopyOrderItem(original));
        }
        return copies;
    }

    private OrderItemEntity deepCopyOrderItem(OrderItemEntity original) {
        if (original == null) return null;
        OrderItemEntity copy = new OrderItemEntity(original.getItemId(), original.getBarcode());
        copy.setOrderId(original.getOrderId());
        copy.setProductId(original.getProductId());
        copy.setProductName(original.getProductName());
        copy.setBuyPrice(original.getBuyPrice());
        copy.setSellPrice(original.getSellPrice());
        copy.setQuantity(original.getQuantity());
        return copy;
    }
}