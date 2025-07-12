package com.javandroid.accounting_app.ui.adapter.customer;

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
    private final CustomerClickListener listener;

    public CustomerListAdapter(CustomerClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CustomerEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<CustomerEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull CustomerEntity oldItem, @NonNull CustomerEntity newItem) {
            return oldItem.getCustomerId() == newItem.getCustomerId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CustomerEntity oldItem, @NonNull CustomerEntity newItem) {
            return oldItem.getName().equals(newItem.getName());
        }
    };

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        CustomerEntity customer = getItem(position);
        holder.bind(customer, listener);
    }

    public static class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCustomerName;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
        }

        public void bind(final CustomerEntity customer, final CustomerClickListener listener) {
            tvCustomerName.setText(customer.getName());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCustomerClick(customer);
                }
            });
        }
    }

    public interface CustomerClickListener {
        void onCustomerClick(CustomerEntity customer);
    }
}