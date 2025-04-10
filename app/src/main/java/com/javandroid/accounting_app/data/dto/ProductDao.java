package com.javandroid.accounting_app.data.dto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.javandroid.accounting_app.data.model.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    void insert(Product product);

    @Query("SELECT * FROM product WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    @Query("SELECT * FROM product")
    List<Product> getAllProducts();
}
