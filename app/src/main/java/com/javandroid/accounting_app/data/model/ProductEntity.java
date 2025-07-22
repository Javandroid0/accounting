package com.javandroid.accounting_app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {

    public ProductEntity(@NonNull String name, @NonNull String barcode) {
        this.name = name;
        this.barcode = barcode;
    }

    @NonNull
    public String name;

    @NonNull
    public String barcode;

    public double buyPrice;

    public double sellPrice;

    public double stock;
    @PrimaryKey(autoGenerate = true)
    public long productId;

    public String imagePath;

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(@NonNull String barcode) {
        this.barcode = barcode;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

}
