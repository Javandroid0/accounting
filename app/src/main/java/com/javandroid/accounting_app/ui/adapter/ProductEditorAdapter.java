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
import com.javandroid.accounting_app.data.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductEditorAdapter extends RecyclerView.Adapter<ProductEditorAdapter.ProductViewHolder> {

    public interface OnProductChangeListener {
        void onPriceChanged(Product product, double newSellPrice, double newBuyPrice);

        void onDelete(Product product);
    }

    private List<Product> productList = new ArrayList<>();
    private final OnProductChangeListener listener;

    public ProductEditorAdapter(OnProductChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    public List<Product> getCurrentProducts() {
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
        Product product = productList.get(position);
        System.out.println(product.getBarcode());
        holder.tvProductName.setText(product.getName());
        holder.etSellPrice.setText(String.valueOf(product.getSellPrice()));
        holder.etBuyPrice.setText(String.valueOf(product.getBuyPrice()));

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
        EditText etSellPrice, etBuyPrice;
        Button btnDelete;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            etSellPrice = itemView.findViewById(R.id.et_price);
            etBuyPrice = itemView.findViewById(R.id.et_buy_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }
    }
}
