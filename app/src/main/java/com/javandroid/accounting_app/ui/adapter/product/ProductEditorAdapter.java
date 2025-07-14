package com.javandroid.accounting_app.ui.adapter.product;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter; // Changed from RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ProductEditorAdapter extends ListAdapter<ProductEntity, ProductEditorAdapter.ProductViewHolder> {
    private static final String TAG = "ProductEditorAdapter";

    public interface OnProductChangeListener {
        void onPriceChanged(ProductEntity product, double newSellPrice, double newBuyPrice);

        void onQuantityChanged(ProductEntity product, double newQuantity);

        void onDelete(ProductEntity product);
    }

    private final OnProductChangeListener listener;
    // This will hold all unsaved modifications.
    private final Map<Long, ProductEntity> modifiedProducts = new HashMap<>();
    // To hold the full list from the ViewModel, used for filtering before submitting to ListAdapter
    private List<ProductEntity> fullProductList = new ArrayList<>();


    public ProductEditorAdapter(OnProductChangeListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Updates the master list of products. This list is used as the source for filtering.
     * After updating the master list, you should typically call filter() to update displayed items.
     *
     * @param products The new full list of products from the ViewModel.
     */
    public void setMasterList(List<ProductEntity> products) {
        this.fullProductList = products != null ? new ArrayList<>(products) : new ArrayList<>();
        // Optionally, clear modifications if the master list is a complete refresh from DB
        // For now, modifications are kept.
        reconcileModificationsWithNewMasterList();
        filter(""); // Apply empty filter to display all items from the new master list initially
    }

    private void reconcileModificationsWithNewMasterList() {
        Map<Long, ProductEntity> newModifiedProducts = new HashMap<>();
        for (ProductEntity masterProduct : fullProductList) {
            if (modifiedProducts.containsKey(masterProduct.getProductId())) {
                // Product still exists, carry over its modification but ensure it's based on the latest masterProduct
                ProductEntity previouslyModified = modifiedProducts.get(masterProduct.getProductId());
                // Create a new modified entity based on the master, then apply previous modifications
                ProductEntity reconciledModification = new ProductEntity(masterProduct.getName(), masterProduct.getBarcode());
                reconciledModification.setProductId(masterProduct.getProductId());
                // Apply the specific fields that were modified
                reconciledModification.setSellPrice(previouslyModified.getSellPrice());
                reconciledModification.setBuyPrice(previouslyModified.getBuyPrice());
                reconciledModification.setStock(previouslyModified.getStock());
                newModifiedProducts.put(masterProduct.getProductId(), reconciledModification);
            }
        }
        modifiedProducts.clear();
        modifiedProducts.putAll(newModifiedProducts);
    }


    /**
     * Filters the master list and submits the filtered result to the ListAdapter.
     *
     * @param query The search query.
     */
    public void filter(String query) {
        List<ProductEntity> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(fullProductList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (ProductEntity product : fullProductList) {
                // Check if the product itself OR a modified version matches
                ProductEntity productToCheck = modifiedProducts.getOrDefault(product.getProductId(), product);
                if (productToCheck.getName().toLowerCase().contains(lowerCaseQuery) ||
                        productToCheck.getBarcode().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(product); // Add original from full list, ViewHolder will display modified if exists
                }
            }
        }
        // Submit the filtered list of original products. ViewHolder will handle displaying modified values.
        super.submitList(filteredList);
    }


    /**
     * Gets the list of products to be saved, including all modifications.
     * It merges the original products with any modifications stored in modifiedProducts.
     *
     * @return A list of ProductEntity objects ready for saving.
     */
    public List<ProductEntity> getProductsToSave() {
        List<ProductEntity> productsToSave = new ArrayList<>();
        // Use the fullProductList as the base, as it's the source of truth from DB/ViewModel
        for (ProductEntity originalProduct : fullProductList) {
            // If a product was modified, add its modified version. Otherwise, add the original.
            productsToSave.add(modifiedProducts.getOrDefault(originalProduct.getProductId(), originalProduct));
        }
        // This list now contains all products from the original list, with modifications applied.
        // It doesn't include products that might have been added only to modifiedProducts map
        // unless they originated from fullProductList (which they should).
        return productsToSave;
    }

    /**
     * Call this after a save operation to clear pending modifications,
     * assuming the save was successful and the ViewModel will provide an updated master list.
     */
    public void clearModifications() {
        modifiedProducts.clear();
    }

    /**
     * Removes an item from the adapter's dataset and underlying lists.
     * This is for UI responsiveness; the actual DB deletion is handled by the ViewModel.
     *
     * @param product The product to remove.
     */
    public void removeItemFromDisplay(ProductEntity product) {
        if (product == null) return;
        fullProductList.removeIf(p -> p.getProductId() == product.getProductId());
        modifiedProducts.remove(product.getProductId());
        filter(getCurrentFilterQuery()); // Re-filter and submit to update ListAdapter
    }

    private String currentFilterQuery = ""; // Store current filter query

    public void setCurrentFilterQuery(String query) {
        this.currentFilterQuery = query != null ? query : "";
    }

    private String getCurrentFilterQuery() {
        return this.currentFilterQuery;
    }


    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_editor, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity originalProduct = getItem(position); // Get original product from ListAdapter's current list
        // Display data from the modified version if it exists, otherwise from the original.
        ProductEntity productToDisplay = modifiedProducts.getOrDefault(originalProduct.getProductId(), originalProduct);
        holder.bind(productToDisplay, originalProduct.getProductId()); // Pass the actual product to display and its stable ID
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        EditText etSellPrice, etBuyPrice, etQuantity;
        android.widget.ImageButton btnDelete;

        private TextWatcher sellPriceWatcher, buyPriceWatcher, quantityWatcher;
        private long currentProductId; // To correctly update the modifiedProducts map

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            etQuantity = itemView.findViewById(R.id.et_quantity);
            etSellPrice = itemView.findViewById(R.id.et_price);
            etBuyPrice = itemView.findViewById(R.id.et_buy_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);

            btnDelete.setOnClickListener(v -> {
                ProductEntity productInList = getItem(getBindingAdapterPosition()); // Get from ListAdapter
                if (productInList != null) {
                    listener.onDelete(productInList); // ViewModel handles DB delete
                    // UI removal will happen when LiveData updates, or call adapter.removeItemFromDisplay() from fragment
                }
            });
        }

        void bind(ProductEntity product, long stableId) {
            this.currentProductId = stableId; // Use the stable ID from the original list item

            clearWatchers();

            tvProductName.setText(product.getName());
            etSellPrice.setText(String.format(Locale.US, "%.2f", product.getSellPrice()));
            etBuyPrice.setText(String.format(Locale.US, "%.2f", product.getBuyPrice()));
            etQuantity.setText(String.format(Locale.US, "%.0f", product.getStock())); // Assuming stock is whole number

            setupWatchers(product);
        }

        private void clearWatchers() {
            if (sellPriceWatcher != null) etSellPrice.removeTextChangedListener(sellPriceWatcher);
            if (buyPriceWatcher != null) etBuyPrice.removeTextChangedListener(buyPriceWatcher);
            if (quantityWatcher != null) etQuantity.removeTextChangedListener(quantityWatcher);
        }

        private void setupWatchers(ProductEntity baseProductForDisplay) {
            sellPriceWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    handleModification(newVal -> {
                        ProductEntity modProduct = getOrCreateModifiedProduct(currentProductId, baseProductForDisplay);
                        modProduct.setSellPrice(newVal);
                        listener.onPriceChanged(modProduct, modProduct.getSellPrice(), modProduct.getBuyPrice());
                    }, s.toString());
                }
            };
            buyPriceWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    handleModification(newVal -> {
                        ProductEntity modProduct = getOrCreateModifiedProduct(currentProductId, baseProductForDisplay);
                        modProduct.setBuyPrice(newVal);
                        listener.onPriceChanged(modProduct, modProduct.getSellPrice(), modProduct.getBuyPrice());
                    }, s.toString());
                }
            };
            quantityWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    handleModification(newVal -> {
                        ProductEntity modProduct = getOrCreateModifiedProduct(currentProductId, baseProductForDisplay);
                        modProduct.setStock((int) newVal.doubleValue()); // Assuming stock is int
                        listener.onQuantityChanged(modProduct, modProduct.getStock());
                    }, s.toString());
                }
            };

            etSellPrice.addTextChangedListener(sellPriceWatcher);
            etBuyPrice.addTextChangedListener(buyPriceWatcher);
            etQuantity.addTextChangedListener(quantityWatcher);
        }

        private ProductEntity getOrCreateModifiedProduct(long productId, ProductEntity baseProduct) {
            return modifiedProducts.computeIfAbsent(productId, k -> {
                // Create a fresh copy from the base product (which is from the ListAdapter, so could be an already modified one if not careful)
                // Better to get from fullProductList using productId to ensure we copy from a clean state or latest display state.
                // For simplicity here, we copy the `baseProduct` which is what's currently displayed.
                ProductEntity newMod = new ProductEntity(baseProduct.getName(), baseProduct.getBarcode());
                newMod.setProductId(baseProduct.getProductId());
                newMod.setSellPrice(baseProduct.getSellPrice());
                newMod.setBuyPrice(baseProduct.getBuyPrice());
                newMod.setStock(baseProduct.getStock());
                return newMod;
            });
        }


        private void handleModification(ModificationConsumer action, String valueStr) {
            if (valueStr.isEmpty()) return; // Or handle as clearing the field
            try {
                double newValue = Double.parseDouble(valueStr);
                action.accept(newValue);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid number format in EditText: " + valueStr);
                // Optionally, revert to previous value or show error
            }
        }
    }

    @FunctionalInterface
    interface ModificationConsumer {
        void accept(Double newValue);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.getProductId() == newItem.getProductId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            // Compare all fields that affect UI. If modifiedProducts handles the true state,
            // this might just compare names/barcodes for structural similarity if view holders recycle complexly.
            // For ListAdapter, if the object reference changes or contents change as per equals(), it updates.
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getBarcode(), newItem.getBarcode()) &&
                    oldItem.getSellPrice() == newItem.getSellPrice() &&
                    oldItem.getBuyPrice() == newItem.getBuyPrice() &&
                    oldItem.getStock() == newItem.getStock();
        }
    };
}