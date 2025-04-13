package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Order> orderList;
    private OnItemClickListener onItemClickListener;

    public ProductAdapter(List<Order> orderList, OnItemClickListener onItemClickListener) {
        this.orderList = orderList;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_order, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.productName.setText(order.getProductName());
        holder.productPrice.setText(String.format("Price: $%.2f", order.getProductSellPrice()));
        holder.productQuantity.setText(String.format("Quantity: %d", order.getQuantity()));
        holder.productTotal.setText(String.format("Total: $%.2f", order.getQuantity() * order.getProductSellPrice()));
        holder.removeButton.setOnClickListener(v -> onItemClickListener.onRemoveClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateOrderList(List<Order> orderList) {
        this.orderList = orderList;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onRemoveClick(Order order);
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        TextView productName, productPrice, productQuantity, productTotal;
        View removeButton;

        public ProductViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
            productTotal = itemView.findViewById(R.id.productTotal);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
