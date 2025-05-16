package com.javandroid.accounting_app.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.ui.adapter.ProductEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProductEditorFragment extends Fragment {
    private ProductViewModel productViewModel;
    private ProductEditorAdapter adapter;
    private ActivityResultLauncher<Intent> csvFilePickerLauncher;
    private static final String TAG = "ProductEditorFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        csvFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            List<ProductEntity> csvProducts = loadProductsFromUri(requireContext(), uri);
                            for (ProductEntity p : csvProducts) {
                                productViewModel.insertProduct(p);
                            }
                            Toast.makeText(getContext(), "Products loaded from selected CSV!", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

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
            public void onPriceChanged(ProductEntity product, double newSellPrice, double newBuyPrice) {
                product.setSellPrice(newSellPrice);
                product.setBuyPrice(newBuyPrice);
            }

            @Override
            public void onQuantityChanged(ProductEntity product, double newQuantity) {
                product.setStock((int) newQuantity);
            }

            @Override
            public void onDelete(ProductEntity product) {
                productViewModel.deleteProduct(product);
            }
        });

        recyclerView.setAdapter(adapter);

        productViewModel.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            adapter.submitList(products);
        });

        btnSave.setOnClickListener(v -> {
            List<ProductEntity> updatedProducts = adapter.getCurrentProducts();
            productViewModel.updateProducts(updatedProducts);
            Toast.makeText(getContext(), "Changes saved successfully!", Toast.LENGTH_SHORT).show();
        });

        Button btnLoadCsv = view.findViewById(R.id.btn_load_csv);
        btnLoadCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            csvFilePickerLauncher.launch(intent);
        });

        EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
        });
    }

    private List<ProductEntity> loadProductsFromUri(Context context, Uri uri) {
        List<ProductEntity> productList = new ArrayList<>();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4) {
                    String barcode = tokens[0].trim();
                    String name = tokens[1].trim();
                    double sellPrice = Double.parseDouble(tokens[2].trim());
                    double buyPrice = Double.parseDouble(tokens[3].trim());
                    int stock = tokens.length > 4 ? Integer.parseInt(tokens[4].trim()) : 0;

                    ProductEntity product = productViewModel.createProduct(name, barcode, buyPrice, sellPrice, stock);
                    productList.add(product);
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return productList;
    }
}
