package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.dao.ProductDao;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final ProductDao productDao;
    private final ExecutorService executor;

    public ProductRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        productDao = db.productDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void insert(ProductEntity product) {
        executor.execute(() -> productDao.insert(product));
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return productDao.getAllProducts();
    }

    public void update(ProductEntity product) {
        executor.execute(() -> productDao.update(product));
    }

    public void update(List<ProductEntity> products) {
        executor.execute(() -> {
            for (ProductEntity product : products) {
                productDao.update(product);
            }
        });
    }

    public void delete(ProductEntity product) {
        executor.execute(() -> productDao.delete(product));
    }

    public void deleteAll() {
        executor.execute(productDao::deleteAll);
    }

    public ProductEntity getProductByBarcodeSync(String barcode) {
        // Only use this in background threads
        return productDao.getProductByBarcodeSync(barcode);
    }

    public ProductEntity getProductByIdSync(long productId) {
        // Only use this in background threads
        return productDao.getProductByIdSync(productId);
    }

    public LiveData<ProductEntity> getProductByBarcode(String barcode) {
        // System.out.println(getProductByBarcodeSync(barcode));
        return productDao.getProductByBarcode(barcode); // assuming you have productDao
    }

    public LiveData<ProductEntity> getProductById(long productId) {
        return productDao.getProductById(productId);
    }

}
