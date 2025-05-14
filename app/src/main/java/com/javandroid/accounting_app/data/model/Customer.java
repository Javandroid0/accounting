package com.javandroid.accounting_app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class Customer {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    // Add other fields like phone, address, etc.

    public Customer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
