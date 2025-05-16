package com.javandroid.accounting_app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class CustomerEntity {

    @PrimaryKey(autoGenerate = true)
    public long customerId;

    @NonNull
    public String name;

    public CustomerEntity() {
        // Empty constructor needed by Room
    }

    public CustomerEntity(long customerId) {
        this.customerId = customerId;
    }

    public CustomerEntity(@NonNull String name) {
        this.name = name;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
