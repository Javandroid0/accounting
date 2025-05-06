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
import com.javandroid.accounting_app.data.model.Order;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.OrderViewHolder> {

    public interface OnOrderAction {
        void onClick(Order order);
    }

    public interface OnQuantityChanged {
        void onQuantityChanged(Order order, double newQuantity);
    }

    public interface OnOrderDeleted {
        void onDelete1(Order order);
    }

    private final OnOrderAction onIncrease;
    private final OnOrderAction onDecrease;
    private final OnQuantityChanged onQuantityChanged;
    private final OnOrderDeleted onOrderDeleted;

    private boolean isBinding = false;

    public OrderAdapter(OnOrderAction onIncrease,
                        OnOrderAction onDecrease,
                        OnQuantityChanged onQuantityChanged,
                        OnOrderDeleted onOrderDeleted) {
        super(DIFF_CALLBACK);
        this.onIncrease = onIncrease;
        this.onDecrease = onDecrease;
        this.onQuantityChanged = onQuantityChanged;
        this.onOrderDeleted = onOrderDeleted;
    }

    private static final DiffUtil.ItemCallback<Order> DIFF_CALLBACK = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.getProductId() == newItem.getProductId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.equals(newItem);
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
        Order order = getItem(position);

        holder.nameView.setText(order.getProductName());
        holder.priceView.setText(String.valueOf(order.getProductSellPrice()));

        // Remove old watcher
        if (holder.quantityView.getTag() instanceof TextWatcher) {
            holder.quantityView.removeTextChangedListener((TextWatcher) holder.quantityView.getTag());
        }

        // Bind quantity safely
        isBinding = true;
        holder.quantityView.setText(String.valueOf(order.getQuantity()));
        isBinding = false;

        // Change icon if quantity = 1
        holder.btnDecrease.setText(order.getQuantity() <= 1 ? "🗑" : "−");

        // Add TextWatcher for manual edits
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isBinding) return;

                String input = s.toString();
                if (!input.isEmpty()) {
                    try {
                        double quantity = Double.parseDouble(input);
                        order.setQuantity(quantity);
                        onQuantityChanged.onQuantityChanged(order, quantity);
                    } catch (NumberFormatException e) {
                        holder.quantityView.setError("Invalid number");
                    }
                }
            }
        };

        holder.quantityView.setTag(watcher);
        holder.quantityView.addTextChangedListener(watcher);

        holder.btnIncrease.setOnClickListener(v -> {
            double newQuantity = order.getQuantity() + 1;
            order.setQuantity(newQuantity);
            notifyItemChanged(holder.getAdapterPosition());
            onQuantityChanged.onQuantityChanged(order, newQuantity);
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (order.getQuantity() <= 1) {
                onOrderDeleted.onDelete1(order);
            } else {
                double newQuantity = order.getQuantity() - 1;
                order.setQuantity(newQuantity);
                notifyItemChanged(holder.getAdapterPosition());
                onQuantityChanged.onQuantityChanged(order, newQuantity);
            }
        });
    }
}
