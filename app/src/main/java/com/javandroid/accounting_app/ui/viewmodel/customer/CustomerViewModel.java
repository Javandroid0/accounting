package com.javandroid.accounting_app.ui.viewmodel.customer;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.repository.CustomerRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerViewModel extends AndroidViewModel {
    private static final String TAG = "CustomerViewModel";

    private final CustomerRepository customerRepository;
    private final MutableLiveData<CustomerEntity> selectedCustomer = new MutableLiveData<>();
    private final LiveData<List<CustomerEntity>> allCustomers;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CustomerViewModel(@NonNull Application application) {
        super(application);
        customerRepository = new CustomerRepository(application);
        allCustomers = customerRepository.getAllCustomers();
    }

    public LiveData<List<CustomerEntity>> getAllCustomers() {
        return allCustomers;
    }

    public void setSelectedCustomer(CustomerEntity customer) {
        if (customer != null) {
            Log.d(TAG, "Setting selected customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");
        } else {
            Log.w(TAG, "Attempted to set null customer");
        }
        selectedCustomer.setValue(customer);
    }

    public LiveData<CustomerEntity> getSelectedCustomer() {
        return selectedCustomer;
    }

    public void insert(CustomerEntity customer) {
        if (customer == null) {
            Log.e(TAG, "Cannot insert null customer");
            return;
        }

        Log.d(TAG, "Inserting customer: " + customer.getName());
        executor.execute(() -> {
            customerRepository.insert(customer);
        });
    }

    public void update(CustomerEntity customer) {
        if (customer == null) {
            Log.e(TAG, "Cannot update null customer");
            return;
        }

        Log.d(TAG, "Updating customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");
        executor.execute(() -> {
            customerRepository.update(customer);
        });
    }

    public void delete(CustomerEntity customer) {
        if (customer == null) {
            Log.e(TAG, "Cannot delete null customer");
            return;
        }

        Log.d(TAG, "Deleting customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");
        executor.execute(() -> {
            customerRepository.delete(customer);
        });
    }

    public void deleteAll() {
        Log.d(TAG, "Deleting all customers");
        executor.execute(() -> {
            customerRepository.deleteAll();
        });
    }

    public LiveData<CustomerEntity> getCustomerById(long customerId) {
        return customerRepository.getCustomerById(customerId);
    }

    /**
     * Get all customers synchronously
     */
    public List<CustomerEntity> getAllCustomersSync() {
        try {
            return customerRepository.getAllCustomersSync();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching customers synchronously", e);
            return null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
