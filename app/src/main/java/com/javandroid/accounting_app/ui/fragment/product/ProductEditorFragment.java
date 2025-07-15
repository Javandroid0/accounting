package com.javandroid.accounting_app.ui.fragment.product;

import android.app.AlertDialog;
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
import com.javandroid.accounting_app.ui.adapter.product.ProductEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.product.ProductViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProductEditorFragment extends Fragment implements ProductEditorAdapter.OnProductInteractionListener {
    private ProductViewModel productViewModel;
    private ProductEditorAdapter adapter;
    private ActivityResultLauncher<Intent> csvFilePickerLauncher;
    private static final String TAG = "ProductEditorFragment";
    private EditText etSearch;
    private com.google.android.material.chip.Chip chipSortByStock;
    private List<ProductEntity> masterProductList = new ArrayList<>();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


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
                            new Thread(() -> {
                                List<ProductEntity> csvProducts = loadProductsFromUri(requireContext(), uri);
                                getActivity().runOnUiThread(() -> {
                                    if (csvProducts != null && !csvProducts.isEmpty()) {
                                        for (ProductEntity p : csvProducts) {
                                            productViewModel.insertProduct(p);
                                        }
                                        Toast.makeText(getContext(), csvProducts.size() + " products processed!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "No products found or error in CSV.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).start();
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
        Button btnLoadCsv = view.findViewById(R.id.btn_load_csv);
        Button btnDeleteAll = view.findViewById(R.id.btn_delete_all_products);
        chipSortByStock = view.findViewById(R.id.chip_sort_by_stock);
        etSearch = view.findViewById(R.id.et_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductEditorAdapter(this);
        recyclerView.setAdapter(adapter);

        productViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                masterProductList.clear();
                masterProductList.addAll(products);
                filterList(etSearch.getText().toString());
            }
        });

        btnLoadCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            csvFilePickerLauncher.launch(intent);
        });

        btnDeleteAll.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete All Products")
                    .setMessage("Are you sure you want to delete all products? This action cannot be undone.")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        productViewModel.deleteAllProducts();
                        Toast.makeText(getContext(), "All products have been deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipSortByStock.setOnClickListener(v -> {
            productViewModel.changeSortOrder(ProductViewModel.SortType.BY_STOCK);
            Toast.makeText(getContext(), "Sorted by lowest stock", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onEditClick(ProductEntity product) {
        adapter.setEditing(product.getProductId());
    }

    @Override
    public void onSaveClick(ProductEntity product) {
        productViewModel.update(product);
        adapter.setEditing(-1L);
        Toast.makeText(getContext(), product.getName() + " saved.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(ProductEntity product) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete '" + product.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    productViewModel.deleteProduct(product);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterList(String query) {
        List<ProductEntity> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        if (lowerCaseQuery.isEmpty()) {
            filteredList.addAll(masterProductList);
        } else {
            for (ProductEntity product : masterProductList) {
                if (product.getName().toLowerCase().contains(lowerCaseQuery) ||
                        product.getBarcode().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(product);
                }
            }
        }
        adapter.submitList(filteredList);
    }

    private List<ProductEntity> loadProductsFromUri(Context context, Uri uri) {
        List<ProductEntity> productList = new ArrayList<>();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream != null ? inputStream : new InputStream() {
                 @Override
                 public int read() throws IOException {
                     return -1;
                 }
             }))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                Log.w(TAG, "CSV file is empty or header is missing.");
                return productList;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 5) {
                    try {
                        String name = tokens[1].trim();
                        String barcode = tokens[2].trim();
                        double buyPrice = Double.parseDouble(tokens[3].trim());
                        double sellPrice = Double.parseDouble(tokens[4].trim());
                        double stock = Double.parseDouble(tokens[5].trim());
                        ProductEntity product = new ProductEntity(name, barcode);
                        product.setBuyPrice(buyPrice);
                        product.setSellPrice(sellPrice);
                        product.setStock(stock);
                        productList.add(product);
                    } catch (Exception e) {
                        Log.e(TAG, "Skipping line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file", e);
            mainThreadHandler.post(() -> Toast.makeText(getContext(), "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
        return productList;
    }
}