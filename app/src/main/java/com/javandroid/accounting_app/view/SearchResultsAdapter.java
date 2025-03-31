package com.javandroid.accounting_app.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.SearchResult;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private final List<SearchResult> results;
    private final OnProductUpdateListener updateListener;

    public interface OnProductUpdateListener {
        void onUpdate(SearchResult product, String newValue);
    }

    public SearchResultsAdapter(List<SearchResult> results, OnProductUpdateListener listener) {
        this.results = results;
        this.updateListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult product = results.get(position);
        holder.editProduct.setText(product.getData());

        holder.updateButton.setOnClickListener(v -> {
            String newValue = holder.editProduct.getText().toString();
            updateListener.onUpdate(product, newValue);
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText editProduct;
        Button updateButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            editProduct = itemView.findViewById(R.id.editProduct);
//            updateButton = itemView.findViewById(R.id.updateButton);
        }
    }
}
