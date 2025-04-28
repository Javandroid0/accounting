package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Product product); // Create

    @Query("SELECT * FROM products")
    LiveData<List<Product>> getAllProducts(); // Read (Live updates)

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcodeSync(String barcode); // Read one

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    LiveData<Product> getProductByBarcode(String barcode);

    @Update
    void update(Product product); // Update

    @Delete
    void delete(Product product); // Delete

    @Query("DELETE FROM products")
    void deleteAll(); // Optional: Delete all

}
