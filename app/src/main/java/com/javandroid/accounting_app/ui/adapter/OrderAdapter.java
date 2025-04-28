package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private List<Order> orderList = new ArrayList<>();
    private final OnOrderAction onIncrease;
    private final OnOrderAction onDecrease;

    public OrderAdapter(OnOrderAction onIncrease, OnOrderAction onDecrease) {
        this.onIncrease = onIncrease;
        this.onDecrease = onDecrease;
    }

    public void submitList(List<Order> orders) {
        orderList.clear();
        orderList.addAll(orders);
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, quantityView, priceView;
        Button btnIncrease, btnDecrease;


        public OrderViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tv_product_name);
            quantityView = itemView.findViewById(R.id.tv_quantity);
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
        holder.quantityView.setText(" " + (int) order.getQuantity());
        holder.priceView.setText(" Price: " + order.getProductSellPrice());

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

