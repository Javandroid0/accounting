package com.javandroid.accounting_app.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders", foreignKeys = {
        @ForeignKey(entity = CustomerEntity.class, parentColumns = "customerId", childColumns = "customerId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = UserEntity.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("customerId"),
        @Index("userId")
})
public class OrderEntity {
    @PrimaryKey(autoGenerate = true)
    public long orderId;

    public String date;

    public double total;

    public long customerId;

    public long userId;

    public boolean isPaid;

    public OrderEntity(String date, double total, long customerId, long userId) {
        this.date = date;
        this.total = total;
        this.customerId = customerId;
        this.userId = userId;
        this.isPaid = true;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getDate() {
        return date;
    }

//    public void setDate(long date) {
//        this.date = date;
//    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}
