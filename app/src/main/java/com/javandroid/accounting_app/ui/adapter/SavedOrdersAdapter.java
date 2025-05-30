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
import com.javandroid.accounting_app.data.model.OrderEntity;

import java.util.ArrayList;
import java.util.List;

public class SavedOrdersAdapter extends ListAdapter<OrderEntity, SavedOrdersAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderEntity order);
    }

    private final OnOrderClickListener listener;
    private List<OrderEntity> originalList;
    private List<OrderEntity> filteredList;

    private static final DiffUtil.ItemCallback<OrderEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<OrderEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.getOrderId() == newItem.getOrderId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.getTotal() == newItem.getTotal() &&
                    oldItem.getDate().equals(newItem.getDate()) &&
                    oldItem.getCustomerId() == newItem.getCustomerId();
        }
    };

    public SavedOrdersAdapter(OnOrderClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.originalList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
    }

    @Override
    public void submitList(List<OrderEntity> list) {
        originalList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        super.submitList(list != null ? new ArrayList<>(list) : null);
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            submitList(originalList);
            return;
        }

        filteredList = new ArrayList<>();
        String trimmedQuery = query.trim();

        try {
            // First try to parse as an integer for exact Order ID matching
            long searchOrderId = Long.parseLong(trimmedQuery);

            // Find orders that exactly match the ID
            for (OrderEntity order : originalList) {
                if (order.getOrderId() == searchOrderId) {
                    filteredList.add(order);
                }
            }

            // If we found exact matches, return only those
            if (!filteredList.isEmpty()) {
                submitList(filteredList);
                return;
            }
        } catch (NumberFormatException e) {
            // Not a valid number, so continue with prefix search
        }

        // If no exact match was found or the query wasn't a number,
        // search by order ID prefix or date
        String lowerCaseQuery = trimmedQuery.toLowerCase();
        for (OrderEntity order : originalList) {
            // Check if query is contained in order ID
            String orderIdString = String.valueOf(order.getOrderId());
            if (orderIdString.startsWith(trimmedQuery)) {
                filteredList.add(order);
                continue;
            }

            // Secondary search: Also check date
            if (order.getDate().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(order);
            }
        }
        submitList(filteredList);
    }

    /**
     * Get the current list of orders being displayed
     *
     * @return The current list of orders
     */
    public List<OrderEntity> getCurrentList() {
        return super.getCurrentList();
    }

    /**
     * Get an order entity at a specific position
     * 
     * @param position The position in the list
     * @return The OrderEntity at the specified position
     */
    public OrderEntity getItem(int position) {
        return super.getItem(position);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderEntity order = getItem(position);
        holder.bind(order);
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView orderIdView;
        private final TextView dateView;
        private final TextView totalView;
        private final TextView customerIdView;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdView = itemView.findViewById(R.id.order_id);
            dateView = itemView.findViewById(R.id.order_date);
            totalView = itemView.findViewById(R.id.order_total);
            customerIdView = itemView.findViewById(R.id.customer_id);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onOrderClick(getItem(position));
                }
            });
        }

        public void bind(OrderEntity order) {
            orderIdView.setText("Order #" + order.getOrderId());
            dateView.setText(order.getDate());
            totalView.setText(String.format("%.2f", order.getTotal()));
            customerIdView.setText("Customer ID: " + order.getCustomerId());
        }
    }
}