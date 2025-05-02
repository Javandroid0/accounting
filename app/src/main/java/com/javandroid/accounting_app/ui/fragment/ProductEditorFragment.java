package com.javandroid.accounting_app.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductEditorFragment extends Fragment {

    private ProductViewModel productViewModel;
    private ProductEditorAdapter adapter;
    private static final String TAG = "ProductEditorFragment";

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
        Button btnLoadCsv = view.findViewById(R.id.btn_load_csv);
        btnLoadCsv.setOnClickListener(v -> {
            List<Product> csvProducts = loadProductsFromCsv(requireContext());
            for (Product p : csvProducts) {
                productViewModel.insertProduct(p);
            }
            Toast.makeText(getContext(), "Products loaded from CSV!", Toast.LENGTH_SHORT).show();
        });

    }

    private List<Product> loadProductsFromCsv(Context context) {
        List<Product> productList = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("products.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isFirstLine = true;
            int i = 0;
            while ((line = reader.readLine()) != null) {
//                if (isFirstLine) {
//                    isFirstLine = false; // Skip header
//                    continue;
//                }
                String[] tokens = line.split(",");
//                Log.d(TAG, "loadProductsFromCsv: " + tokens.length);
                if (tokens.length >= 4) {
                    Product product = new Product();
                    product.setBarcode(tokens[0].trim());
                    product.setName(tokens[1].trim());
                    product.setSellPrice(Double.parseDouble(tokens[2].trim()));
                    product.setBuyPrice(Double.parseDouble(tokens[3].trim()));
//                    product.setDescription(tokens[4].trim());
                    productList.add(product);
//                    Log.d(TAG, "loadProductsFromCsv: " + productList.get(i));
                }
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return productList;
    }


}
