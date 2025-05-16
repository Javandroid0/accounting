package com.javandroid.accounting_app.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderItemEntity;

public class OrderAdapter extends ListAdapter<OrderItemEntity, OrderAdapter.OrderViewHolder> {

    public interface OnOrderItemAction {
        void onClick(OrderItemEntity orderItem);
    }

    public interface OnQuantityChanged {
        void onQuantityChanged(OrderItemEntity orderItem, double newQuantity);
    }

    public interface OnOrderItemDeleted {
        void onDelete(OrderItemEntity orderItem);
    }

    private final OnOrderItemAction onIncrease;
    private final OnOrderItemAction onDecrease;
    private final OnQuantityChanged onQuantityChanged;
    private final OnOrderItemDeleted onOrderItemDeleted;

    private boolean isBinding = false;

    public OrderAdapter(OnOrderItemAction onIncrease,
                        OnOrderItemAction onDecrease,
                        OnQuantityChanged onQuantityChanged,
                        OnOrderItemDeleted onOrderItemDeleted) {
        super(DIFF_CALLBACK);
        this.onIncrease = onIncrease;
        this.onDecrease = onDecrease;
        this.onQuantityChanged = onQuantityChanged;
        this.onOrderItemDeleted = onOrderItemDeleted;
    }

    private static final DiffUtil.ItemCallback<OrderItemEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<OrderItemEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderItemEntity oldItem, @NonNull OrderItemEntity newItem) {
            return oldItem.getItemId() == newItem.getItemId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderItemEntity oldItem, @NonNull OrderItemEntity newItem) {
            return oldItem.getItemId() == newItem.getItemId() &&
                    oldItem.getQuantity() == newItem.getQuantity() &&
                    oldItem.getSellPrice() == newItem.getSellPrice() &&
                    oldItem.getProductName().equals(newItem.getProductName());
        }
    };

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, priceView;
        EditText quantityView;
        Button btnIncrease, btnDecrease;

        public OrderViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tv_product_name);
            priceView = itemView.findViewById(R.id.tv_product_price);
            quantityView = itemView.findViewById(R.id.quantityView);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
        }
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderItemEntity orderItem = getItem(position);

        holder.nameView.setText(orderItem.getProductName());
        holder.priceView.setText(String.valueOf(orderItem.getSellPrice()));

        // Remove old watcher
        if (holder.quantityView.getTag() instanceof TextWatcher) {
            holder.quantityView.removeTextChangedListener((TextWatcher) holder.quantityView.getTag());
        }

        // Bind quantity safely
        isBinding = true;
        holder.quantityView.setText(String.valueOf(orderItem.getQuantity()));
        isBinding = false;

        // Change icon if quantity = 1
        holder.btnDecrease.setText(orderItem.getQuantity() <= 1 ? "🗑" : "−");

        // Add TextWatcher for manual edits
        TextWatcher watcher = createQuantityWatcher(orderItem, holder.quantityView);
        holder.quantityView.setTag(watcher);
        holder.quantityView.addTextChangedListener(watcher);

        holder.btnIncrease.setOnClickListener(v -> {
            double newQuantity = orderItem.getQuantity() + 1;
            orderItem.setQuantity(newQuantity);
            notifyItemChanged(holder.getBindingAdapterPosition());
            onQuantityChanged.onQuantityChanged(orderItem, newQuantity);
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (orderItem.getQuantity() <= 1) {
                onOrderItemDeleted.onDelete(orderItem);
            } else {
                double newQuantity = orderItem.getQuantity() - 1;
                orderItem.setQuantity(newQuantity);
                notifyItemChanged(holder.getBindingAdapterPosition());
                onQuantityChanged.onQuantityChanged(orderItem, newQuantity);
            }
        });
    }

    private TextWatcher createQuantityWatcher(OrderItemEntity orderItem, EditText quantityView) {
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

                String input = s.toString();
                if (!input.isEmpty()) {
                    try {
                        int quantity = Integer.parseInt(input);
                        orderItem.setQuantity(quantity);
                        onQuantityChanged.onQuantityChanged(orderItem, quantity);
                    } catch (NumberFormatException e) {
                        quantityView.setError("Invalid number");
                    }
                }
            }
        };
    }
}
