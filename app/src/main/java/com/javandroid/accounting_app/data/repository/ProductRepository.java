package com.javandroid.accounting_app.data.repository;

import android.content.Context;
import android.util.Log;

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
    private static final String TAG = "ProductRepository";

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
        executor.execute(() -> {
            Log.d(TAG, "Updating single product: " + product.getName() + ", ID=" + product.getProductId());
            productDao.update(product);
        });
    }


    public void update(List<ProductEntity> products) {
        executor.execute(() -> {
            Log.d(TAG, "Updating " + products.size() + " products");
            try {
                // First, try to update each product individually with more detailed logging
                for (ProductEntity product : products) {
                    try {
                        Log.d(TAG, "Updating product: " + product.getName() +
                                ", ID=" + product.getProductId() +
                                ", Stock=" + product.getStock() +
                                ", SellPrice=" + product.getSellPrice() +
                                ", BuyPrice=" + product.getBuyPrice());
                        productDao.update(product);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error updating product " + product.getName() + ": " + ex.getMessage(), ex);
                    }
                }

                // Then try the bulk update as a separate step for redundancy
                try {
                    productDao.updateAll(products);
                    Log.d(TAG, "Bulk update also completed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Bulk update failed, but individual updates were already attempted: " + e.getMessage(),
                            e);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in product update process: " + e.getMessage(), e);
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
        return productDao.getProductByBarcode(barcode);
    }

    public LiveData<ProductEntity> getProductById(long productId) {
        return productDao.getProductById(productId);
    }

    // Add this new method to your ProductRepository.java
    public LiveData<List<ProductEntity>> getAllProductsSortedByStock() {
        return productDao.getAllProductsSortedByStock();
    }
}
