package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.ArrayList;
import java.util.List;

public class ProductEditorAdapter extends RecyclerView.Adapter<ProductEditorAdapter.ProductViewHolder> {

    public interface OnProductChangeListener {
        void onPriceChanged(ProductEntity product, double newSellPrice, double newBuyPrice);

        void onQuantityChanged(ProductEntity product, double newQuantity);

        void onDelete(ProductEntity product);
    }

    private List<ProductEntity> productList = new ArrayList<>();
    private final OnProductChangeListener listener;

    public ProductEditorAdapter(OnProductChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProductEntity> products) {
        this.fullList = new ArrayList<>(products);
        this.productList = new ArrayList<>(products);
        notifyDataSetChanged();
    }

    public List<ProductEntity> getCurrentProducts() {
        return productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_editor, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = productList.get(position);
        System.out.println(product.getBarcode());
        holder.tvProductName.setText(product.getName());
        holder.etSellPrice.setText(String.valueOf(product.getSellPrice()));
        holder.etBuyPrice.setText(String.valueOf(product.getBuyPrice()));
        holder.etQuantity.setText(String.valueOf(product.getStock()));

        holder.etQuantity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double newQuantity = Double.parseDouble(holder.etQuantity.getText().toString());
                    listener.onQuantityChanged(product, newQuantity);
                } catch (NumberFormatException ignored) {
                }
            }
        });

        holder.etSellPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double newSellPrice = Double.parseDouble(holder.etSellPrice.getText().toString());
                    double newBuyPrice = Double.parseDouble(holder.etBuyPrice.getText().toString());
                    listener.onPriceChanged(product, newSellPrice, newBuyPrice);
                } catch (NumberFormatException ignored) {
                }
            }
        });

        holder.etBuyPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double newSellPrice = Double.parseDouble(holder.etSellPrice.getText().toString());
                    double newBuyPrice = Double.parseDouble(holder.etBuyPrice.getText().toString());
                    listener.onPriceChanged(product, newSellPrice, newBuyPrice);
                } catch (NumberFormatException ignored) {
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        EditText etSellPrice, etBuyPrice, etQuantity;
        Button btnDelete;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            etQuantity = itemView.findViewById(R.id.et_quantity);
            etSellPrice = itemView.findViewById(R.id.et_price);
            etBuyPrice = itemView.findViewById(R.id.et_buy_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }
    }

    private List<ProductEntity> fullList = new ArrayList<>();

    public void filter(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            productList = new ArrayList<>(fullList);
        } else {
            List<ProductEntity> filtered = new ArrayList<>();
            for (ProductEntity p : fullList) {
                if (p.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getBarcode().toLowerCase().contains(keyword.toLowerCase())) {
                    filtered.add(p);
                }
            }
            productList = filtered;
        }
        notifyDataSetChanged();
    }

}
