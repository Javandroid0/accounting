package com.javandroid.accounting_app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.view.utility.ProductDiffCallback;

public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductViewHolder> {

    public ProductAdapter() {
        super(new ProductDiffCallback());
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, price, barcode;

        public ProductViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewProductName);
            price = itemView.findViewById(R.id.textViewProductPrice);
            barcode = itemView.findViewById(R.id.textViewProductBarcode);
        }

        public void bind(Product product) {
            name.setText(product.getName());
            price.setText("Price: " + product.getSellPrice());
            barcode.setText("Barcode: " + product.getBarcode());
        }
    }
}
