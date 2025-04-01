package com.javandroid.accounting_app.view.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.service.excel.ProductExcelManager;
import com.javandroid.accounting_app.view.adapter.ProductAdapter;
import com.javandroid.accounting_app.viewmodel.ProductViewModel;
import java.io.IOException;

public class ProductActivity extends AppCompatActivity {

    private ProductViewModel productViewModel;

    private ProductAdapter productAdapter;
    private ProductExcelManager productExcelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productAdapter = new ProductAdapter();
        recyclerView.setAdapter(productAdapter);

        productExcelManager = new ProductExcelManager(this);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        // Observe LiveData from ViewModel
        productViewModel.getProducts().observe(this, productAdapter::submitList);

        // Load products from Excel
        loadProducts();
    }

    private void loadProducts() {
        try {
            productViewModel.loadProducts(productExcelManager.readProducts());
        } catch (IOException e) {
            Log.e("ProductActivity", "Error loading products", e);
            Toast.makeText(this, "Failed to load products", Toast.LENGTH_SHORT).show();
        }
    }
}
