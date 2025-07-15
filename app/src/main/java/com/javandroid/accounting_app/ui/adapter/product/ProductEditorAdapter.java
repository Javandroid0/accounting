package com.javandroid.accounting_app.ui.adapter.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.Locale;
import java.util.Objects;

public class ProductEditorAdapter extends ListAdapter<ProductEntity, ProductEditorAdapter.ProductViewHolder> {

    public interface OnProductInteractionListener {
        void onEditClick(ProductEntity product);

        void onSaveClick(ProductEntity product);

        void onDeleteClick(ProductEntity product);
    }

    private final OnProductInteractionListener listener;
    private long editingProductId = -1L;

    public ProductEditorAdapter(OnProductInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setEditing(long productId) {
        long previouslyEditing = editingProductId;
        editingProductId = productId;
        if (previouslyEditing != -1) {
            notifyItemChanged(findPositionById(previouslyEditing));
        }
        if (editingProductId != -1) {
            notifyItemChanged(findPositionById(productId));
        }
    }

    private int findPositionById(long productId) {
        for (int i = 0; i < getCurrentList().size(); i++) {
            if (getItem(i).getProductId() == productId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_editor, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = getItem(position);
        holder.bind(product);
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        EditText etSellPrice, etBuyPrice, etQuantity;
        ImageButton btnDelete;
        Button btnSaveItem;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            etQuantity = itemView.findViewById(R.id.et_quantity);
            etSellPrice = itemView.findViewById(R.id.et_price);
            etBuyPrice = itemView.findViewById(R.id.et_buy_price);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
            btnSaveItem = itemView.findViewById(R.id.btn_save_item);
        }

        void bind(ProductEntity product) {
            boolean isEditing = product.getProductId() == editingProductId;

            tvProductName.setText(product.getName());
            etSellPrice.setText(String.format(Locale.US, "%.2f", product.getSellPrice()));
            etBuyPrice.setText(String.format(Locale.US, "%.2f", product.getBuyPrice()));
            etQuantity.setText(String.format(Locale.US, "%.0f", product.getStock()));

            etSellPrice.setEnabled(isEditing);
            etBuyPrice.setEnabled(isEditing);
            etQuantity.setEnabled(isEditing);
            btnSaveItem.setVisibility(isEditing ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (!isEditing) {
                    listener.onEditClick(product);
                }
            });

            btnDelete.setOnClickListener(v -> listener.onDeleteClick(product));

            btnSaveItem.setOnClickListener(v -> {
                try {
                    String name = tvProductName.getText().toString();
                    String barcode = product.getBarcode();
                    double newBuyPrice = Double.parseDouble(etBuyPrice.getText().toString());
                    double newSellPrice = Double.parseDouble(etSellPrice.getText().toString());
                    double newStock = Double.parseDouble(etQuantity.getText().toString());

                    ProductEntity updatedProduct = new ProductEntity(name, barcode);
                    updatedProduct.setProductId(product.getProductId());
                    updatedProduct.setBuyPrice(newBuyPrice);
                    updatedProduct.setSellPrice(newSellPrice);
                    updatedProduct.setStock(newStock);

                    listener.onSaveClick(updatedProduct);
                } catch (NumberFormatException e) {
                    // Error handling for invalid number input can be added here
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.getProductId() == newItem.getProductId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    oldItem.getSellPrice() == newItem.getSellPrice() &&
                    oldItem.getBuyPrice() == newItem.getBuyPrice() &&
                    oldItem.getStock() == newItem.getStock();
        }
    };
}