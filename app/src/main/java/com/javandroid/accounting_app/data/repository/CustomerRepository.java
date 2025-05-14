package com.javandroid.accounting_app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.dao.CustomerDao;
import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.Customer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerRepository {

    private final CustomerDao customerDao;
    private final LiveData<List<Customer>> allCustomers;
    private final ExecutorService executorService;

    public CustomerRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        customerDao = db.customerDao();
        allCustomers = customerDao.getAllCustomers();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Customer>> getAllCustomers() {
        return allCustomers;
    }

    public void insert(Customer customer) {
        executorService.execute(() -> customerDao.insert(customer));
    }

    public void update(Customer customer) {
        executorService.execute(() -> customerDao.update(customer));
    }

    public void delete(Customer customer) {
        executorService.execute(() -> customerDao.delete(customer));
    }
}
