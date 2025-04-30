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
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderAction {
        void onClick(Order order);
    }
    public interface OnQuantityChanged {
        void onQuantityChanged(Order order, double newQuantity);
    }

    private List<Order> orderList = new ArrayList<>();
    private final OnOrderAction onIncrease;
    private final OnOrderAction onDecrease;
    private final OnQuantityChanged onQuantityChanged;  // 🔥 New field

    private boolean isBinding = false;


    public OrderAdapter(OnOrderAction onIncrease, OnOrderAction onDecrease, OnQuantityChanged onQuantityChanged) {
        this.onIncrease = onIncrease;
        this.onDecrease = onDecrease;
        this.onQuantityChanged = onQuantityChanged;
    }


    public void submitList(List<Order> orders) {
        orderList.clear();
        orderList.addAll(orders);
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, priceView;
        Button btnIncrease, btnDecrease;
        EditText quantityView;

        public OrderViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tv_product_name);
            quantityView = itemView.findViewById(R.id.quantityView);
            priceView = itemView.findViewById(R.id.tv_product_price);
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
        Order order = orderList.get(position);

        holder.nameView.setText(order.getProductName());
        holder.priceView.setText(String.valueOf(order.getProductSellPrice()));

        // Remove previous TextWatcher
        if (holder.quantityView.getTag() instanceof TextWatcher) {
            holder.quantityView.removeTextChangedListener((TextWatcher) holder.quantityView.getTag());
        }

        // ⚠️ Set binding flag true
        isBinding = true;
        holder.quantityView.setText(String.valueOf(order.getQuantity()));
        isBinding = false;

        // New watcher
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isBinding) return; // ✅ Ignore changes during binding

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

        // Increase
        holder.btnIncrease.setOnClickListener(v -> {
            double newQuantity = order.getQuantity() + 1;
            order.setQuantity(newQuantity);
            notifyItemChanged(holder.getAdapterPosition());
            onQuantityChanged.onQuantityChanged(order, newQuantity);
        });

        // Decrease
        holder.btnDecrease.setOnClickListener(v -> {
            double newQuantity = Math.max(1, order.getQuantity() - 1);
            order.setQuantity(newQuantity);
            notifyItemChanged(holder.getAdapterPosition());
            onQuantityChanged.onQuantityChanged(order, newQuantity);
        });
    }


    @Override
    public int getItemCount() {
        return orderList.size();
    }
}

