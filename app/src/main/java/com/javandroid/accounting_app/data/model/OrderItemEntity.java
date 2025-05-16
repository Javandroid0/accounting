package com.javandroid.accounting_app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items", foreignKeys = {
        @ForeignKey(entity = OrderEntity.class, parentColumns = "orderId", childColumns = "orderId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = ProductEntity.class, parentColumns = "productId", childColumns = "productId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index("orderId"),
        @Index("productId")
})
public class OrderItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long itemId;

    public Long orderId;

    public Long productId; // Nullable: Product might get deleted but we keep the order record

    @NonNull
    public String productName;

    @NonNull
    public String barcode;

    public double buyPrice;

    public double sellPrice;

    public double quantity;

    public OrderItemEntity(long itemId, @NonNull String barcode) {
        this.itemId = itemId;
        this.barcode = barcode;
    }

    @NonNull
    public String getProductName() {
        return productName;
    }

    public void setProductName(@NonNull String productName) {
        this.productName = productName;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}
