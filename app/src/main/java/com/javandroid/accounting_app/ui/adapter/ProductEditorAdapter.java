package com.javandroid.accounting_app.ui.adapter;

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
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductEditorAdapter extends RecyclerView.Adapter<ProductEditorAdapter.ProductViewHolder> {
    private static final String TAG = "ProductEditorAdapter";

    public interface OnProductChangeListener {
        void onPriceChanged(ProductEntity product, double newSellPrice, double newBuyPrice);

        void onQuantityChanged(ProductEntity product, double newQuantity);

        void onDelete(ProductEntity product);
    }

    private List<ProductEntity> productList = new ArrayList<>();
    private final OnProductChangeListener listener;
    private List<ProductEntity> fullList = new ArrayList<>();

    // Track modified products
    private final Map<Long, ProductEntity> modifiedProducts = new HashMap<>();

    public ProductEditorAdapter(OnProductChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProductEntity> products) {
        this.fullList = new ArrayList<>(products);
        this.productList = new ArrayList<>(products);
        notifyDataSetChanged();
    }

    public List<ProductEntity> getCurrentProducts() {
        // Combine original list with modified products to ensure all changes are saved
        List<ProductEntity> result = new ArrayList<>();

        for (ProductEntity product : fullList) {
            // If this product was modified, use the modified version
            if (modifiedProducts.containsKey(product.getProductId())) {
                result.add(modifiedProducts.get(product.getProductId()));
                Log.d(TAG, "Including modified product: " + product.getName() +
                        " with updated values: sellPrice=" + modifiedProducts.get(product.getProductId()).getSellPrice()
                        +
                        ", buyPrice=" + modifiedProducts.get(product.getProductId()).getBuyPrice() +
                        ", stock=" + modifiedProducts.get(product.getProductId()).getStock());
            } else {
                result.add(product);
                Log.d(TAG, "Including unmodified product: " + product.getName());
            }
        }

        Log.d(TAG, "Returning " + result.size() + " products for update, with " +
                modifiedProducts.size() + " modifications");
        return result;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_editor, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = productList.get(position);

        // Remove previous TextWatchers to prevent recursive calls
        holder.clearTextWatchers();

        // Set data
        holder.tvProductName.setText(product.getName());
        holder.etSellPrice.setText(String.valueOf(product.getSellPrice()));
        holder.etBuyPrice.setText(String.valueOf(product.getBuyPrice()));
        holder.etQuantity.setText(String.valueOf(product.getStock()));

        // Set up new event handlers
        holder.setupTextWatchers(product);
        holder.btnDelete.setOnClickListener(v -> {
            listener.onDelete(product);
            modifiedProducts.remove(product.getProductId());
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        EditText etSellPrice, etBuyPrice, etQuantity;
        Button btnDelete;

        private TextWatcher sellPriceWatcher;
        private TextWatcher buyPriceWatcher;
        private TextWatcher quantityWatcher;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            etQuantity = itemView.findViewById(R.id.et_quantity);
            etSellPrice = itemView.findViewById(R.id.et_price);
            etBuyPrice = itemView.findViewById(R.id.et_buy_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }

        void clearTextWatchers() {
            if (sellPriceWatcher != null) {
                etSellPrice.removeTextChangedListener(sellPriceWatcher);
            }
            if (buyPriceWatcher != null) {
                etBuyPrice.removeTextChangedListener(buyPriceWatcher);
            }
            if (quantityWatcher != null) {
                etQuantity.removeTextChangedListener(quantityWatcher);
            }
        }

        void setupTextWatchers(ProductEntity product) {
            // Create and set TextWatchers for immediate feedback
            sellPriceWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        try {
                            double newSellPrice = Double.parseDouble(s.toString());
                            updateProductPrice(product, newSellPrice, product.getBuyPrice());
                        } catch (NumberFormatException e) {
                            // Ignore parse errors during typing
                        }
                    }
                }
            };

            buyPriceWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        try {
                            double newBuyPrice = Double.parseDouble(s.toString());
                            updateProductPrice(product, product.getSellPrice(), newBuyPrice);
                        } catch (NumberFormatException e) {
                            // Ignore parse errors during typing
                        }
                    }
                }
            };

            quantityWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        try {
                            double newQuantity = Double.parseDouble(s.toString());
                            updateProductQuantity(product, newQuantity);
                        } catch (NumberFormatException e) {
                            // Ignore parse errors during typing
                        }
                    }
                }
            };

            etSellPrice.addTextChangedListener(sellPriceWatcher);
            etBuyPrice.addTextChangedListener(buyPriceWatcher);
            etQuantity.addTextChangedListener(quantityWatcher);
        }

        private void updateProductPrice(ProductEntity product, double newSellPrice, double newBuyPrice) {
            // Create a copy of the product with updated values
            ProductEntity updatedProduct = getUpdatedProduct(product);
            updatedProduct.setSellPrice(newSellPrice);
            updatedProduct.setBuyPrice(newBuyPrice);

            // Add to modified products map
            modifiedProducts.put(product.getProductId(), updatedProduct);

            // Notify listener
            listener.onPriceChanged(updatedProduct, newSellPrice, newBuyPrice);
            Log.d(TAG, "Price updated for " + product.getName() + ", ID=" + product.getProductId() +
                    ": sell=" + newSellPrice + ", buy=" + newBuyPrice);
        }

        private void updateProductQuantity(ProductEntity product, double newQuantity) {
            // Create a copy of the product with updated values
            ProductEntity updatedProduct = getUpdatedProduct(product);
            updatedProduct.setStock((int) newQuantity);

            // Add to modified products map
            modifiedProducts.put(product.getProductId(), updatedProduct);

            // Notify listener
            listener.onQuantityChanged(updatedProduct, newQuantity);
            Log.d(TAG, "Quantity updated for " + product.getName() + ", ID=" + product.getProductId() +
                    ": quantity=" + newQuantity);
        }

        private ProductEntity getUpdatedProduct(ProductEntity original) {
            // If we already have a modified version, use that as base
            if (modifiedProducts.containsKey(original.getProductId())) {
                return modifiedProducts.get(original.getProductId());
            }

            // Otherwise, create a new product instance with the same data
            ProductEntity copy = new ProductEntity(original.getName(), original.getBarcode());
            copy.setProductId(original.getProductId());
            copy.setBuyPrice(original.getBuyPrice());
            copy.setSellPrice(original.getSellPrice());
            copy.setStock(original.getStock());
            return copy;
        }
    }

    // Helper class to reduce boilerplate in TextWatcher
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    public void filter(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            productList = new ArrayList<>(fullList);
        } else {
            List<ProductEntity> filtered = new ArrayList<>();
            for (ProductEntity p : fullList) {
                if (p.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getBarcode().toLowerCase().contains(keyword.toLowerCase())) {
                    filtered.add(p);
                }
            }
            productList = filtered;
        }
        notifyDataSetChanged();
    }
}
