package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.List;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ProductViewHolder> {

    private List<ProductEntity> productList;
    private OnItemClickListener onItemClickListener;

    public ProductListAdapter(List<ProductEntity> productList, OnItemClickListener onItemClickListener) {
        this.productList = productList;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        ProductEntity product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format("Price: %.2f", product.getSellPrice()));
        holder.itemView.setOnClickListener(v -> onItemClickListener.onProductClick(product));
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateProductList(List<ProductEntity> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onProductClick(ProductEntity product);
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        TextView productName, productPrice;

        public ProductViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
        }
    }
}
