package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orderList = new ArrayList<>();

    public void submitList(List<Order> orders) {
        orderList.clear();
        orderList.addAll(orders);
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, quantityView, priceView;

        public OrderViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.product_name);
            quantityView = itemView.findViewById(R.id.product_quantity);
            priceView = itemView.findViewById(R.id.product_price);
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
        holder.quantityView.setText("Qty: " + order.getQuantity());
        holder.priceView.setText("Price: " + order.getProductSellPrice());
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }
}
