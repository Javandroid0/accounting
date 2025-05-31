package com.javandroid.accounting_app.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.databinding.ItemOrderEditorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderEditorAdapter extends ListAdapter<OrderItemEntity, OrderEditorAdapter.OrderItemViewHolder> {

    private final OrderItemInteractionListener listener; // Changed to common interface
    private boolean isEditable = false;
    private static final String TAG = "OrderEditorAdapter";

    private static final DiffUtil.ItemCallback<OrderItemEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<OrderItemEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderItemEntity oldItem, @NonNull OrderItemEntity newItem) {
            return oldItem.getItemId() == newItem.getItemId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderItemEntity oldItem, @NonNull OrderItemEntity newItem) {
            return oldItem.getQuantity() == newItem.getQuantity() &&
                    oldItem.getSellPrice() == newItem.getSellPrice() &&
                    oldItem.getProductName().equals(newItem.getProductName());
        }
    };

    public OrderEditorAdapter(OrderItemInteractionListener listener) { // Changed to common interface
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        if (this.isEditable != editable) {
            this.isEditable = editable;
            notifyDataSetChanged();
            Log.d(TAG, "Adapter edit mode set to: " + this.isEditable);
        }
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderEditorBinding binding = ItemOrderEditorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        // Pass the listener and a way to get the current editable state
        return new OrderItemViewHolder(binding, listener, () -> isEditable);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItemEntity item = getItem(position);
        holder.bind(item);
    }

    // Kept IsEditableProvider internal as it's closely tied to this adapter's functionality
    interface IsEditableProvider {
        boolean isEditable();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderEditorBinding binding;
        private final OrderItemInteractionListener listener; // Changed to common interface
        private final IsEditableProvider isEditableProvider;
        private OrderItemEntity currentItem;
        private TextWatcher quantityWatcher;
        private TextWatcher priceWatcher;
        private boolean isCurrentlyBindingVH = false;

        OrderItemViewHolder(ItemOrderEditorBinding binding, OrderItemInteractionListener listener, IsEditableProvider isEditableProvider) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.isEditableProvider = isEditableProvider;
            setupStaticListeners();
        }

        private void setupStaticListeners() {
            binding.decreaseButton.setOnClickListener(v -> {
                if (currentItem != null && isEditableProvider.isEditable()) {
                    if (currentItem.getQuantity() <= 1) {
                        listener.onDelete(currentItem);
                    } else {
                        double newQuantity = currentItem.getQuantity() - 1;
                        isCurrentlyBindingVH = true;
                        binding.quantityEdit.setText(String.format(Locale.US, "%.0f", newQuantity));
                        updateDecreaseButtonAppearanceLocal(newQuantity);
                        isCurrentlyBindingVH = false;
                        listener.onQuantityChanged(currentItem, newQuantity);
                    }
                }
            });

            binding.increaseButton.setOnClickListener(v -> {
                if (currentItem != null && isEditableProvider.isEditable()) {
                    double newQuantity = currentItem.getQuantity() + 1;
                    isCurrentlyBindingVH = true;
                    binding.quantityEdit.setText(String.format(Locale.US, "%.0f", newQuantity));
                    updateDecreaseButtonAppearanceLocal(newQuantity);
                    isCurrentlyBindingVH = false;
                    listener.onQuantityChanged(currentItem, newQuantity);
                }
            });
        }

        private void updateDecreaseButtonAppearanceLocal(double quantity) {
            if (isEditableProvider.isEditable()) { // Check if actually editable
                binding.decreaseButton.setText(quantity <= 1 ? "🗑️" : "−");
            } else {
                binding.decreaseButton.setText("−"); // Default appearance if somehow visible when not editable
            }
        }

        public void bind(OrderItemEntity item) {
            this.currentItem = item;
            isCurrentlyBindingVH = true;

            binding.productName.setText(item.getProductName());
            if (item.getQuantity() == Math.floor(item.getQuantity())) {
                binding.quantityEdit.setText(String.format(Locale.US, "%.0f", item.getQuantity()));
            } else {
                binding.quantityEdit.setText(String.valueOf(item.getQuantity()));
            }
            binding.priceEdit.setText(String.format(Locale.US, "%.2f", item.getSellPrice()));

            boolean editableNow = isEditableProvider.isEditable();
            binding.quantityEdit.setEnabled(editableNow);
            binding.priceEdit.setEnabled(editableNow); // Assuming price is editable
            binding.increaseButton.setVisibility(editableNow ? View.VISIBLE : View.GONE);
            binding.decreaseButton.setVisibility(editableNow ? View.VISIBLE : View.GONE);
            if (editableNow) {
                updateDecreaseButtonAppearanceLocal(item.getQuantity());
            }


            if (quantityWatcher != null)
                binding.quantityEdit.removeTextChangedListener(quantityWatcher);
            if (priceWatcher != null) binding.priceEdit.removeTextChangedListener(priceWatcher);

            if (editableNow) {
                quantityWatcher = createQuantityWatcherLocal(item);
                priceWatcher = createPriceWatcherLocal(item); // If price is editable
                binding.quantityEdit.addTextChangedListener(quantityWatcher);
                binding.priceEdit.addTextChangedListener(priceWatcher);
            } else {
                quantityWatcher = null;
                priceWatcher = null;
            }
            isCurrentlyBindingVH = false;
        }

        private TextWatcher createQuantityWatcherLocal(OrderItemEntity itemRef) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isCurrentlyBindingVH || !isEditableProvider.isEditable()) return;
                    if (s.length() > 0) {
                        try {
                            double newQuantity = Double.parseDouble(s.toString());
                            if (Math.abs(itemRef.getQuantity() - newQuantity) > 0.001) {
                                listener.onQuantityChanged(itemRef, newQuantity);
                                updateDecreaseButtonAppearanceLocal(newQuantity);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "NFE q:" + s);
                        }
                    }
                }
            };
        }

        private TextWatcher createPriceWatcherLocal(OrderItemEntity itemRef) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isCurrentlyBindingVH || !isEditableProvider.isEditable()) return;
                    if (s.length() > 0) {
                        try {
                            double newPrice = Double.parseDouble(s.toString());
                            if (Math.abs(itemRef.getSellPrice() - newPrice) > 0.001) {
                                listener.onPriceChanged(itemRef, newPrice);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "NFE p:" + s);
                        }
                    }
                }
            };
        }
    }
}