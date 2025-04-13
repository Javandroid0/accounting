package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.Product;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProductRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void insert(Product product) {
        executor.execute(() -> db.productDao().insert(product));
    }

    public LiveData<List<Product>> getAllProducts() {
        return db.productDao().getAllProducts();
    }

    public void update(Product product) {
        executor.execute(() -> db.productDao().update(product));
    }

    public void delete(Product product) {
        executor.execute(() -> db.productDao().delete(product));
    }

    public void deleteAll() {
        executor.execute(() -> db.productDao().deleteAll());
    }

    public Product getProductByBarcodeSync(String barcode) {
        // Only use this in background threads
        return db.productDao().getProductByBarcodeSync(barcode);
    }
    public LiveData<Product> getProductByBarcode(String barcode) {
        return db.productDao().getProductByBarcode(barcode); // assuming you have productDao
    }

}
