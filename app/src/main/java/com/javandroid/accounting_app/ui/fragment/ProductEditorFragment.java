package com.javandroid.accounting_app.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private EditText etSearch;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        csvFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            List<ProductEntity> csvProducts = loadProductsFromUri(requireContext(), uri);
                            if (csvProducts != null && !csvProducts.isEmpty()) {
                                for (ProductEntity p : csvProducts) {
                                    // This will insert or replace based on primary key if products have IDs,
                                    // or insert as new if IDs are 0.
                                    // Consider if CSV products should always be treated as new or if they can update existing.
                                    productViewModel.insertProduct(p);
                                }
                                Toast.makeText(getContext(), csvProducts.size() + " products processed from CSV!", Toast.LENGTH_SHORT).show();
                                // LiveData should refresh the list.
                            } else {
                                Toast.makeText(getContext(), "No products found or error in CSV.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_products);
        Button btnSaveChanges = view.findViewById(R.id.btn_save_product_changes);
        Button btnLoadCsv = view.findViewById(R.id.btn_load_csv);
        etSearch = view.findViewById(R.id.et_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ProductEditorAdapter(new ProductEditorAdapter.OnProductChangeListener() {
            @Override
            public void onPriceChanged(ProductEntity productWithChanges, double newSellPrice, double newBuyPrice) {
                // Listener called by adapter after it updates its internal 'modifiedProducts' map.
                // No explicit action needed here by the fragment for these events if adapter handles state.
                Log.d(TAG, "Fragment Listener: Price changed for " + productWithChanges.getName());
            }

            @Override
            public void onQuantityChanged(ProductEntity productWithChanges, double newQuantity) {
                Log.d(TAG, "Fragment Listener: Quantity changed for " + productWithChanges.getName());
            }

            @Override
            public void onDelete(ProductEntity productToDelete) {
                // ViewModel handles DB deletion. LiveData will update the list.
                productViewModel.deleteProduct(productToDelete);
                // For immediate UI update, tell adapter to remove from its display source and re-filter
                adapter.removeItemFromDisplay(productToDelete); // New method in adapter
                Toast.makeText(getContext(), productToDelete.getName() + " deleted.", Toast.LENGTH_SHORT).show();

            }
        });
        recyclerView.setAdapter(adapter);

        productViewModel.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                Log.d(TAG, "Full product list updated from ViewModel: " + products.size() + " items.");
                adapter.setMasterList(products); // Update adapter's master list
                // The filter in setMasterList will call submitList on ListAdapter.
            }
        });

        btnSaveChanges.setOnClickListener(v -> {
            List<ProductEntity> productsToSave = adapter.getProductsToSave();
            if (!productsToSave.isEmpty()) {
                // Check if there are actual modifications to save to avoid unnecessary DB calls
                boolean hasActualModifications = false;
                for (ProductEntity p : productsToSave) {
                    // A more robust check would compare each product with its original state from fullProductList
                    // For now, we rely on modifiedProducts map being non-empty if changes were made.
                    // The getProductsToSave already merges, so any product from it needs saving if different from original.
                    // We can refine this by comparing productsToSave with adapter.fullProductList based on ID.
                }
                // For simplicity now: if adapter.getProductsToSave() yields items that differ from original fullProductList,
                // or if modifiedProducts map was non-empty.
                // The `getProductsToSave` method itself implies these are the ones needing potential DB update.

                productViewModel.updateProducts(productsToSave); // ViewModel's updateProducts updates all in list
                adapter.clearModifications(); // Clear pending changes after saving
                Toast.makeText(getContext(), productsToSave.size() + " product(s) changes saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No changes to save.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLoadCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*"); // More specific like "text/csv" if possible and reliable
            csvFilePickerLauncher.launch(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                adapter.setCurrentFilterQuery(query); // Store query for removeItemFromDisplay
                adapter.filter(query);
            }
        });
    }

    private List<ProductEntity> loadProductsFromUri(Context context, Uri uri) {
        List<ProductEntity> productList = new ArrayList<>();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream != null ? inputStream : new InputStream() {
                 @Override
                 public int read() throws IOException {
                     return -1;
                 }
             }))) { // Handle possible null inputStream

            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                Log.w(TAG, "CSV file is empty or header is missing.");
                return productList; // Empty list
            }
            Log.d(TAG, "CSV Header: " + headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                // Basic CSV parsing, assumes no commas within fields themselves or properly quoted fields.
                // A robust CSV parser library is better for complex CSVs.
                String[] tokens = line.split(",");
                if (tokens.length >= 5) { // Expecting at least: (some ID), name, barcode, buyPrice, sellPrice, [stock]
                    try {
                        // Assuming CSV structure: ID, Name, Barcode, BuyPrice, SellPrice, Stock
                        // Or if ID is not in CSV and should be auto-generated: Name, Barcode, BuyPrice, SellPrice, Stock
                        // The original code had: barcode=tokens[2], name=tokens[1], sellPrice=tokens[4], buyPrice=tokens[3], stock=tokens[5]
                        // This implies tokens[0] might be an ID from CSV or just ignored.
                        // Let's stick to the structure implied by original token indexing.

                        String name = tokens[1].trim();
                        String barcode = tokens[2].trim();
                        double buyPrice = Double.parseDouble(tokens[3].trim());
                        double sellPrice = Double.parseDouble(tokens[4].trim());
                        // Stock is optional in the original parsing logic (tokens.length > 4)
                        // but it should be tokens.length > 5 for tokens[5]
                        int stock = (tokens.length > 5 && !tokens[5].trim().isEmpty()) ? Integer.parseInt(tokens[5].trim()) : 0;

                        // ProductEntity constructor: ProductEntity(@NonNull String name, @NonNull String barcode)
                        ProductEntity product = new ProductEntity(name, barcode);
                        product.setBuyPrice(buyPrice);
                        product.setSellPrice(sellPrice);
                        product.setStock(stock);
                        // If CSV contains an ID that should be used (e.g., tokens[0]):
                        // product.setProductId(Long.parseLong(tokens[0].trim()));
                        // Otherwise, productId will be 0 and auto-generated on insert if it's new.
                        productList.add(product);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Skipping line due to number format error: " + line, e);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Skipping line due to insufficient columns: " + line, e);
                    }
                } else {
                    Log.w(TAG, "Skipping malformed CSV line: " + line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file", e);
            mainThreadHandler.post(() -> Toast.makeText(getContext(), "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show());
            return new ArrayList<>(); // Return empty on error
        }
        return productList;
    }

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

}