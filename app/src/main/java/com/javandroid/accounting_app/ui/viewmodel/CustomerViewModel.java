package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.repository.CustomerRepository;

import java.util.List;

public class CustomerViewModel extends AndroidViewModel {

    private final CustomerRepository customerRepository;
    private final MutableLiveData<CustomerEntity> selectedCustomer = new MutableLiveData<>();
    private final LiveData<List<CustomerEntity>> allCustomers;

    public CustomerViewModel(@NonNull Application application) {
        super(application);
        customerRepository = new CustomerRepository(application);
        allCustomers = customerRepository.getAllCustomers();
    }

    public LiveData<List<CustomerEntity>> getAllCustomers() {
        return allCustomers;
    }

    public LiveData<List<CustomerEntity>> getCustomers() {
        return allCustomers;
    }

    public void setSelectedCustomer(CustomerEntity customer) {
        selectedCustomer.setValue(customer);
    }

    public void selectCustomer(CustomerEntity customer) {
        selectedCustomer.setValue(customer);
    }

    public LiveData<CustomerEntity> getSelectedCustomer() {
        return selectedCustomer;
    }

    public void insert(CustomerEntity customer) {
        customerRepository.insert(customer);
    }

    public void update(CustomerEntity customer) {
        customerRepository.update(customer);
    }

    public void delete(CustomerEntity customer) {
        customerRepository.delete(customer);
    }

    public void deleteAll() {
        customerRepository.deleteAll();
    }

    public LiveData<CustomerEntity> getCustomerById(long customerId) {
        return customerRepository.getCustomerById(customerId);
    }
}
