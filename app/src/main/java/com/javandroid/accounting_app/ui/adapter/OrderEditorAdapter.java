package com.javandroid.accounting_app.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderItemEntity;

import java.util.ArrayList;
import java.util.List;

public class OrderEditorAdapter extends ListAdapter<OrderItemEntity, OrderEditorAdapter.OrderEditorViewHolder> {

    private final OnOrderItemChangeListener listener;
    private List<OrderItemEntity> originalList;
    private List<OrderItemEntity> filteredList;

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

    public OrderEditorAdapter(OnOrderItemChangeListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.originalList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
    }

    @Override
    public void submitList(List<OrderItemEntity> list) {
        originalList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        super.submitList(list);
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            submitList(originalList);
            return;
        }

        filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (OrderItemEntity item : originalList) {
            if (item.getProductName().toLowerCase().contains(lowerCaseQuery) ||
                    item.getBarcode().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(item);
            }
        }
        submitList(filteredList);
    }

    @NonNull
    @Override
    public OrderEditorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_editor, parent, false);
        return new OrderEditorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderEditorViewHolder holder, int position) {
        OrderItemEntity item = getItem(position);
        holder.bind(item);
    }

    public interface OnOrderItemChangeListener {
        void onQuantityChanged(OrderItemEntity item, double newQuantity);

        void onPriceChanged(OrderItemEntity item, double newPrice);

        void onDelete(OrderItemEntity item);
    }

    class OrderEditorViewHolder extends RecyclerView.ViewHolder {
        private final TextView productNameView;
        private final EditText quantityEdit;
        private final EditText priceEdit;
        private final ImageButton decreaseButton;
        private final ImageButton increaseButton;
        //        private final Button deleteButton;
        private OrderItemEntity currentItem;

        public OrderEditorViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameView = itemView.findViewById(R.id.product_name);
            quantityEdit = itemView.findViewById(R.id.quantity_edit);
            priceEdit = itemView.findViewById(R.id.price_edit);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            increaseButton = itemView.findViewById(R.id.increase_button);
//            deleteButton = itemView.findViewById(R.id.delete_button);

            setupListeners();
        }

        private void setupListeners() {
            quantityEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    quantityEdit.selectAll();
                }
            });

            quantityEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (currentItem != null && s.length() > 0) {
                        try {
                            double newQuantity = Double.parseDouble(s.toString());
                            listener.onQuantityChanged(currentItem, newQuantity);
                        } catch (NumberFormatException e) {
                            quantityEdit.setText(String.valueOf(currentItem.getQuantity()));
                        }
                    }
                }
            });

            priceEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (currentItem != null && s.length() > 0) {
                        try {
                            double newPrice = Double.parseDouble(s.toString());
                            listener.onPriceChanged(currentItem, newPrice);
                        } catch (NumberFormatException e) {
                            priceEdit.setText(String.valueOf(currentItem.getSellPrice()));
                        }
                    }
                }
            });

            decreaseButton.setOnClickListener(v -> {
                if (currentItem != null) {
                    if (currentItem.getQuantity() <= 1) {
                        listener.onDelete(currentItem);
                    } else {
                        double newQuantity = currentItem.getQuantity() - 1;
                        quantityEdit.setText(String.valueOf(newQuantity));
                        listener.onQuantityChanged(currentItem, newQuantity);
                    }
                }
            });

            increaseButton.setOnClickListener(v -> {
                if (currentItem != null) {
                    double newQuantity = currentItem.getQuantity() + 1;
                    quantityEdit.setText(String.valueOf(newQuantity));
                    listener.onQuantityChanged(currentItem, newQuantity);
                }
            });
//
//            deleteButton.setOnClickListener(v -> {
//                if (currentItem != null) {
//                    listener.onDelete(currentItem);
//                }
//            });
        }

        public void bind(OrderItemEntity item) {
            currentItem = item;
            productNameView.setText(item.getProductName());
            quantityEdit.setText(String.valueOf(item.getQuantity()));
            priceEdit.setText(String.valueOf(item.getSellPrice()));

            if (item.getQuantity() <= 1) {
                decreaseButton.setImageResource(android.R.drawable.ic_menu_delete);
            } else {
                decreaseButton.setImageResource(android.R.drawable.ic_menu_add);
            }
        }
    }
}
