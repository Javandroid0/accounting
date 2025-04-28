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
//        holder.quantityView.setText(" " + (int) order.getQuantity());

        holder.quantityView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.isEmpty()) {
                    try {
                        double quantity = Double.parseDouble(input);
                        onQuantityChanged.onQuantityChanged(order, quantity);  // 🔥 Correct
                    } catch (NumberFormatException e) {
                        holder.quantityView.setError("Invalid number");
                    }
                }
            }
        });


        holder.priceView.setText(" " + order.getProductSellPrice());

//        holder.itemView.setOnClickListener(v -> onIncrease.onClick(order));
//        holder.itemView.setOnLongClickListener(v -> {
//            onDecrease.onClick(order);
//            return true;
//        });
        holder.btnIncrease.setOnClickListener(v -> onIncrease.onClick(order));
        holder.btnDecrease.setOnClickListener(v -> onDecrease.onClick(order));

    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }
}

