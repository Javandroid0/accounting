package com.javandroid.accounting_app.ui.adapter.customer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.data.model.CustomerEntity;

import java.util.ArrayList;
import java.util.List;

public class CustomerDrawerAdapter extends RecyclerView.Adapter<CustomerDrawerAdapter.CustomerViewHolder> {

    private List<CustomerEntity> customers = new ArrayList<>();
    private final OnCustomerClickListener listener;

    public CustomerDrawerAdapter(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CustomerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        CustomerEntity currentCustomer = customers.get(position);
        holder.textView.setText(currentCustomer.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCustomerClick(currentCustomer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public void setCustomers(List<CustomerEntity> customers) {
        this.customers = customers;
        notifyDataSetChanged();
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        CustomerViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }

    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerEntity customer);
    }
}