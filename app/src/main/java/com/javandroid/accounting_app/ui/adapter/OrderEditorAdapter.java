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
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderEditorAdapter extends RecyclerView.Adapter<OrderEditorAdapter.OrderEditorViewHolder> {

    public interface OnOrderChangeListener {
        void onQuantityChanged(Order order, double newQuantity);
        void onPriceChanged(Order order, double newPrice);
        void onDelete(Order order);
    }

    private final List<Order> orderList = new ArrayList<>();
    private final OnOrderChangeListener listener;

    public OrderEditorAdapter(OnOrderChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> orders) {
        orderList.clear();
        orderList.addAll(orders);
        notifyDataSetChanged();
    }

    public List<Order> getCurrentOrders() {
        return orderList;
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
        Order order = orderList.get(position);

        holder.nameText.setText(order.getProductName());

        holder.quantityInput.setText(String.valueOf(order.getQuantity()));
        holder.priceInput.setText(String.valueOf(order.getProductSellPrice()));

        holder.quantityInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    double newQuantity = Double.parseDouble(s.toString());
                    listener.onQuantityChanged(order, newQuantity);
                } catch (NumberFormatException e) { /* Ignore invalid */ }
            }
        });

        holder.priceInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    double newPrice = Double.parseDouble(s.toString());
                    listener.onPriceChanged(order, newPrice);
                } catch (NumberFormatException e) { /* Ignore invalid */ }
            }
        });

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderEditorViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        EditText quantityInput, priceInput;
        Button btnDelete;

        public OrderEditorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_product_name);
            quantityInput = itemView.findViewById(R.id.et_quantity);
            priceInput = itemView.findViewById(R.id.et_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_order);
        }
    }
}
