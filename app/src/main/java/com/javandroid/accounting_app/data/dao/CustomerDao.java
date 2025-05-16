package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.javandroid.accounting_app.data.model.CustomerEntity;

import java.util.List;

@Dao
public interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY customerId ASC")
    LiveData<List<CustomerEntity>> getAllCustomers();

    @Query("SELECT * FROM customers ORDER BY customerId ASC")
    List<CustomerEntity> getAllCustomersSync();

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    CustomerEntity getCustomerByIdSync(long customerId);

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    LiveData<CustomerEntity> getCustomerById(long customerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CustomerEntity customer);

    @Update
    void update(CustomerEntity customer);

    @Delete
    void delete(CustomerEntity customer);

    @Query("DELETE FROM customers")
    void deleteAll();
}
