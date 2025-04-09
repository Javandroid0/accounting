package com.javandroid.accounting_app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList = new ArrayList<>();

    public ProductAdapter(List<Product> products) {
        this.productList = products;
    }

    public void updateProducts(List<Product> newProducts) {
        this.productList.clear();
        this.productList.addAll(newProducts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName, textViewSellPrice, textViewBuyPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewProductName);
            textViewSellPrice = itemView.findViewById(R.id.textViewProductPrice);
            textViewBuyPrice = itemView.findViewById(R.id.textViewProductBarcode);
        }

        public void bind(Product product) {
            textViewName.setText(product.getName());
            textViewSellPrice.setText("Sell: $" + product.getSellPrice());
            textViewBuyPrice.setText("Buy: $" + product.getBuyPrice());
        }
    }
}
