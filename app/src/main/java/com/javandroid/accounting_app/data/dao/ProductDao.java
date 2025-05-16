package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product); // Create

    @Query("SELECT * FROM products ORDER BY productId ASC")
    LiveData<List<ProductEntity>> getAllProducts(); // Read (Live updates)

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    LiveData<ProductEntity> getProductById(long productId);

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    ProductEntity getProductByIdSync(long productId);

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcodeSync(String barcode); // Read one

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    LiveData<ProductEntity> getProductByBarcode(String barcode);

    @Update
    void update(ProductEntity product); // Update

    @Update
    void updateAll(List<ProductEntity> products);

    @Delete
    void delete(ProductEntity product); // Delete

    @Query("DELETE FROM products")
    void deleteAll(); // Optional: Delete all

}
