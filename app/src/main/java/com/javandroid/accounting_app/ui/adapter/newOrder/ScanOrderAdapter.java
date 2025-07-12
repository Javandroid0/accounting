package com.javandroid.accounting_app.ui.adapter.newOrder;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.databinding.ItemOrderEditorBinding; // Assuming this layout is general enough
import com.javandroid.accounting_app.ui.adapter.order.OrderItemInteractionListener;

import java.util.Locale;

public class ScanOrderAdapter extends ListAdapter<OrderItemEntity, ScanOrderAdapter.OrderItemViewHolderScan> {

    private final OrderItemInteractionListener listener;
    // private List<OrderItemEntity> originalList; // Not strictly needed if submitList handles it well
    // private List<OrderItemEntity> filteredList; // Filter logic can be external or internal
    private boolean isBindingInternal = false; // Renamed to avoid conflict if used elsewhere
    private static final String TAG = "ScanOrderAdapter";

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

    public ScanOrderAdapter(OrderItemInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // submitList is inherited from ListAdapter

    @NonNull
    @Override
    public OrderItemViewHolderScan onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderEditorBinding binding = ItemOrderEditorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderItemViewHolderScan(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolderScan holder, int position) {
        OrderItemEntity item = getItem(position);
        holder.bind(item);
    }

    static class OrderItemViewHolderScan extends RecyclerView.ViewHolder {
        private final ItemOrderEditorBinding binding;
        private final OrderItemInteractionListener listener;
        private OrderItemEntity currentItem;
        private TextWatcher quantityWatcher;
        private TextWatcher priceWatcher;
        private boolean isCurrentlyBindingVH = false;


        OrderItemViewHolderScan(ItemOrderEditorBinding binding, OrderItemInteractionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            setupStaticListeners();
        }

        private void setupStaticListeners() {
            binding.decreaseButton.setOnClickListener(v -> {
                if (currentItem != null) { // Always editable context for ScanOrder
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

            binding.decreaseButton.setOnLongClickListener(v -> {
                if (currentItem != null) {
                    listener.onDelete(currentItem);
                    return true;
                }
                return false;
            });

            binding.increaseButton.setOnClickListener(v -> {
                if (currentItem != null) { // Always editable context
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
            binding.decreaseButton.setText(quantity <= 1 ? "🗑️" : "−");
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

            // For ScanOrderAdapter, fields are always enabled.
            binding.quantityEdit.setEnabled(true);
            binding.priceEdit.setEnabled(true); // Or false if price is not set here
            binding.increaseButton.setVisibility(View.VISIBLE);
            binding.decreaseButton.setVisibility(View.VISIBLE);
            updateDecreaseButtonAppearanceLocal(item.getQuantity());


            if (quantityWatcher != null)
                binding.quantityEdit.removeTextChangedListener(quantityWatcher);
            if (priceWatcher != null) binding.priceEdit.removeTextChangedListener(priceWatcher);

            quantityWatcher = createQuantityWatcherLocal(item);
            priceWatcher = createPriceWatcherLocal(item);
            binding.quantityEdit.addTextChangedListener(quantityWatcher);
            binding.priceEdit.addTextChangedListener(priceWatcher);

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
                    if (isCurrentlyBindingVH) return;
                    if (s.length() > 0) {
                        try {
                            double newQuantity = Double.parseDouble(s.toString());
                            if (Math.abs(itemRef.getQuantity() - newQuantity) > 0.001) {
                                listener.onQuantityChanged(itemRef, newQuantity);
                                updateDecreaseButtonAppearanceLocal(newQuantity);
                            }
                        } catch (NumberFormatException e) { /* ignore */ }
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
                    if (isCurrentlyBindingVH) return;
                    if (s.length() > 0) {
                        try {
                            double newPrice = Double.parseDouble(s.toString());
                            if (Math.abs(itemRef.getSellPrice() - newPrice) > 0.001) {
                                listener.onPriceChanged(itemRef, newPrice);
                            }
                        } catch (NumberFormatException e) { /* ignore */ }
                    }
                }
            };
        }
    }
}