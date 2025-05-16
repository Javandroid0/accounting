package com.javandroid.accounting_app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;

public class CustomerListAdapter extends ListAdapter<CustomerEntity, CustomerListAdapter.CustomerViewHolder> {

    private final OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerEntity customer);
    }

    public CustomerListAdapter(OnCustomerClickListener listener) {
        super(new DiffUtil.ItemCallback<CustomerEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull CustomerEntity oldItem, @NonNull CustomerEntity newItem) {
                return oldItem.getCustomerId() == newItem.getCustomerId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull CustomerEntity oldItem, @NonNull CustomerEntity newItem) {
                return oldItem.getName().equals(newItem.getName());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        CustomerEntity customer = getItem(position);
        holder.bind(customer, listener);
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
        }

        void bind(CustomerEntity customer, OnCustomerClickListener listener) {
            tvName.setText(customer.getName());
            itemView.setOnClickListener(v -> listener.onCustomerClick(customer));
        }
    }
}