package com.javandroid.accounting_app.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.databinding.ItemOrderEditorBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized adapter for the OrderDetailsFragment for viewing and editing
 * existing orders
 */
public class OrderDetailsAdapter extends ListAdapter<OrderItemEntity, RecyclerView.ViewHolder> {

    private final OnOrderItemChangeListener listener;
    private List<OrderItemEntity> originalList;
    private List<OrderItemEntity> filteredList;
    private boolean isBinding = false;
    private static final String TAG = "OrderDetailsAdapter";

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

    public OrderDetailsAdapter(OnOrderItemChangeListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.originalList = new ArrayList<>();
//        this.filteredList = new ArrayList<>();
    }

    @Override
    public void submitList(List<OrderItemEntity> list) {
        if (list != null) {
            originalList = new ArrayList<>(list);
            super.submitList(new ArrayList<>(list));
        } else {
            originalList = new ArrayList<>();
            super.submitList(null);
        }
    }

//    public void filter(String query) {
//        if (query == null || query.isEmpty()) {
//            submitList(originalList);
//            return;
//        }
//
//        filteredList = new ArrayList<>();
//        String lowerCaseQuery = query.toLowerCase().trim();
//
//        for (OrderItemEntity item : originalList) {
//            if (item.getProductName().toLowerCase().contains(lowerCaseQuery) ||
//                    item.getBarcode().toLowerCase().contains(lowerCaseQuery)) {
//                filteredList.add(item);
//            }
//        }
//        submitList(filteredList);
//    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderEditorBinding binding = ItemOrderEditorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        OrderItemEntity item = getItem(position);
        ((OrderItemViewHolder) holder).bind(item);
    }

    public interface OnOrderItemChangeListener {
        void onQuantityChanged(OrderItemEntity item, double newQuantity);

        void onPriceChanged(OrderItemEntity item, double newPrice);

        void onDelete(OrderItemEntity item);
    }

    class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderEditorBinding binding;
        private OrderItemEntity currentItem;
        private TextWatcher quantityWatcher;
        private TextWatcher priceWatcher;

        OrderItemViewHolder(ItemOrderEditorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Instead of using a non-existent deleteButton, we'll use the decrease button
            // for now
            // as a temporary solution
            binding.decreaseButton.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    OrderItemEntity item = getItem(position);
                    listener.onDelete(item);
                    return true;
                }
                return false;
            });

            // Set up quantity adjustment with existing buttons
            binding.decreaseButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    OrderItemEntity item = getItem(position);
                    double newQuantity = Math.max(0, item.getQuantity() - 1);
                    binding.quantityEdit.setText(String.valueOf(newQuantity));
                }
            });

            binding.increaseButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    OrderItemEntity item = getItem(position);
                    double newQuantity = item.getQuantity() + 1;
                    binding.quantityEdit.setText(String.valueOf(newQuantity));
                }
            });
        }

        public void bind(OrderItemEntity item) {
            // Store current item
            currentItem = item;

            // Remove old watchers if they exist
            if (quantityWatcher != null) {
                binding.quantityEdit.removeTextChangedListener(quantityWatcher);
            }
            if (priceWatcher != null) {
                binding.priceEdit.removeTextChangedListener(priceWatcher);
            }

            // Set data with binding flag to prevent callbacks
            isBinding = true;
            binding.productName.setText(item.getProductName());

            // Format quantity as integer if it's a whole number
            if (item.getQuantity() == Math.floor(item.getQuantity())) {
                binding.quantityEdit.setText(String.format("%.0f", item.getQuantity()));
            } else {
                binding.quantityEdit.setText(String.valueOf(item.getQuantity()));
            }

            // Format price as integer if it's a whole number
            if (item.getSellPrice() == Math.floor(item.getSellPrice())) {
                binding.priceEdit.setText(String.format("%.0f", item.getSellPrice()));
            } else {
                binding.priceEdit.setText(String.valueOf(item.getSellPrice()));
            }

            isBinding = false;

            // Create and add new watchers
            quantityWatcher = createQuantityWatcher(item);
            priceWatcher = createPriceWatcher(item);

            binding.quantityEdit.addTextChangedListener(quantityWatcher);
            binding.priceEdit.addTextChangedListener(priceWatcher);
        }

        private TextWatcher createQuantityWatcher(OrderItemEntity item) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isBinding)
                        return;

                    if (s.length() > 0) {
                        try {
                            double newQuantity = Double.parseDouble(s.toString());
                            listener.onQuantityChanged(item, newQuantity);
                        } catch (NumberFormatException e) {
                            isBinding = true;
                            binding.quantityEdit.setText(String.valueOf(item.getQuantity()));
                            isBinding = false;
                        }
                    }
                }
            };
        }

        private TextWatcher createPriceWatcher(OrderItemEntity item) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isBinding)
                        return;

                    if (s.length() > 0) {
                        try {
                            double newPrice = Double.parseDouble(s.toString());
                            listener.onPriceChanged(item, newPrice);
                        } catch (NumberFormatException e) {
                            isBinding = true;
                            binding.priceEdit.setText(String.valueOf(item.getSellPrice()));
                            isBinding = false;
                        }
                    }
                }
            };
        }
    }
}