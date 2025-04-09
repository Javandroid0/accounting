package com.javandroid.accounting_app.view.utility;

import androidx.recyclerview.widget.DiffUtil;
import com.javandroid.accounting_app.model.Product;

public class ProductDiffCallback extends DiffUtil.ItemCallback<Product> {

    @Override
    public boolean areItemsTheSame(Product oldItem, Product newItem) {
        return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(Product oldItem, Product newItem) {
        return oldItem.equals(newItem);
    }
}
