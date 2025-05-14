package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.Customer;
import com.javandroid.accounting_app.data.repository.CustomerRepository;

import java.util.List;

public class CustomerViewModel extends AndroidViewModel {

    private final LiveData<List<Customer>> customers;
    private final MutableLiveData<Customer> selectedCustomer = new MutableLiveData<>();

    private final CustomerRepository repository;

    public CustomerViewModel(@NonNull Application application) {
        super(application);
        repository = new CustomerRepository(application);
        customers = repository.getAllCustomers();
    }

    public LiveData<List<Customer>> getCustomers() {
        return customers;
    }

    public void selectCustomer(Customer customer) {
        selectedCustomer.setValue(customer);
    }

    public LiveData<Customer> getSelectedCustomer() {
        return selectedCustomer;
    }
}
