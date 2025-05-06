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

    private ActivityResultLauncher<Intent> csvFilePickerLauncher;
    private static final String TAG = "ProductEditorFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        csvFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            List<Product> csvProducts = loadProductsFromUri(requireContext(), uri);
                            for (Product p : csvProducts) {
                                productViewModel.insertProduct(p);
                            }
                            Toast.makeText(getContext(), "Products loaded from selected CSV!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


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
//        btnLoadCsv.setOnClickListener(v -> {
//            List<Product> csvProducts = loadProductsFromCsv(requireContext());
//            for (Product p : csvProducts) {
//                productViewModel.insertProduct(p);
//            }
//            Toast.makeText(getContext(), "Products loaded from CSV!", Toast.LENGTH_SHORT).show();
//        });

        btnLoadCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/*"); // You can use "text/csv" or "application/csv"
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


    private List<Product> loadProductsFromUri(Context context, Uri uri) {
        List<Product> productList = new ArrayList<>();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4) {
                    Product product = new Product();
                    product.setBarcode(tokens[0].trim());
                    product.setName(tokens[1].trim());
                    product.setSellPrice(Double.parseDouble(tokens[2].trim()));
                    product.setBuyPrice(Double.parseDouble(tokens[3].trim()));
                    productList.add(product);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return productList;
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
