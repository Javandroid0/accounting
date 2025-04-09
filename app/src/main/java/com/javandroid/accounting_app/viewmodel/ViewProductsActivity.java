package com.javandroid.accounting_app.viewmodel;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.service.excel.ProductExcelManager;
import com.javandroid.accounting_app.view.adapter.ProductAdapter;
import java.io.IOException;
import java.util.List;

public class ViewProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ProductExcelManager productExcelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_products);

        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productExcelManager = new ProductExcelManager(this);
        loadProducts();
    }

    private void loadProducts() {
        try {
            List<Product> productList = productExcelManager.readProducts();
            adapter = new ProductAdapter(productList);
            recyclerView.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
