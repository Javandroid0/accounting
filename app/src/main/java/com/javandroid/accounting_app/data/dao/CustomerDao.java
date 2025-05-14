package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.javandroid.accounting_app.data.model.Customer;

import java.util.List;

@Dao
public interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY id ASC")
    LiveData<List<Customer>> getAllCustomers();

    @Insert
    void insert(Customer customer);

    @Update
    void update(Customer customer);

    @Delete
    void delete(Customer customer);
}
