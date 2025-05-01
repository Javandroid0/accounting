package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.ui.adapter.ProductEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;

import java.util.List;

public class ProductEditorFragment extends Fragment {

    private ProductViewModel productViewModel;
    private ProductEditorAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_products);
        Button btnSave = view.findViewById(R.id.btn_save_product_changes);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        adapter = new ProductEditorAdapter(new ProductEditorAdapter.OnProductChangeListener() {
            @Override
            public void onPriceChanged(Product product, double newSellPrice, double newBuyPrice) {
                product.setSellPrice(newSellPrice);
                product.setBuyPrice(newBuyPrice);
            }

            @Override
            public void onDelete(Product product) {
                productViewModel.deleteProduct(product);
            }
        });

        recyclerView.setAdapter(adapter);

        productViewModel.getAllProducts1().observe(getViewLifecycleOwner(), products -> {
            adapter.submitList(products);
        });

        btnSave.setOnClickListener(v -> {
            List<Product> updatedProducts = adapter.getCurrentProducts();
            productViewModel.updateProducts(updatedProducts);
        });
    }
}
