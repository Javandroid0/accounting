package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.dao.CustomerDao;
import com.javandroid.accounting_app.data.model.CustomerEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerRepository {
    private final CustomerDao customerDao;
    private final ExecutorService executor;
    private final AppDatabase db;

    public CustomerRepository(Context context) {
        db = AppDatabase.getInstance(context);
        customerDao = db.customerDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<CustomerEntity>> getAllCustomers() {
        return customerDao.getAllCustomers();
    }

    public void insert(CustomerEntity customer) {
        executor.execute(() -> customerDao.insert(customer));
    }

    public void update(CustomerEntity customer) {
        executor.execute(() -> customerDao.update(customer));
    }

    public void delete(CustomerEntity customer) {
        executor.execute(() -> customerDao.delete(customer));
    }

    public void deleteAll() {
        executor.execute(() -> customerDao.deleteAll());
    }

    public void getCustomerByIdSync(long customerId, OnCustomerResultCallback callback) {
        executor.execute(() -> {
            CustomerEntity customer = customerDao.getCustomerByIdSync(customerId);
            callback.onResult(customer);
        });
    }

    public LiveData<CustomerEntity> getCustomerById(long customerId) {
        return customerDao.getCustomerById(customerId);
    }

    /**
     * Get all customers synchronously
     */
    public List<CustomerEntity> getAllCustomersSync() {
        return db.customerDao().getAllCustomersSync();
    }

    public interface OnCustomerResultCallback {
        void onResult(CustomerEntity customer);
    }
}
