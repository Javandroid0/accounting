package com.javandroid.accounting_app.view.utility;

import androidx.recyclerview.widget.DiffUtil;
import com.javandroid.accounting_app.model.Product;

public class ProductDiffCallback extends DiffUtil.ItemCallback<Product> {

    @Override
    public boolean areItemsTheSame(Product oldItem, Product newItem) {
        // Compare unique identifiers (like ID or barcode) to see if the items are the same
        return oldItem.getId() == newItem.getId(); // Assuming 'id' is unique
    }

    @Override
    public boolean areContentsTheSame(Product oldItem, Product newItem) {
        // Compare all fields to see if the content of the items is the same
        return oldItem.getName().equals(newItem.getName()) &&
                oldItem.getBarcode().equals(newItem.getBarcode()) &&
                oldItem.getSellPrice() == newItem.getSellPrice() &&
                oldItem.getBuyPrice() == newItem.getBuyPrice();
    }
}
