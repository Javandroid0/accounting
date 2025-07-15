package com.javandroid.accounting_app.ui.adapter.order;

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

    public enum SearchField {
        ALL,
        ID,
        DATE,
        CUSTOMER,
        USER
    }

    private final OnOrderClickListener listener;
    private List<OrderEntity> originalList;

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
    }

    @Override
    public void submitList(List<OrderEntity> list) {
        originalList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        super.submitList(list != null ? new ArrayList<>(list) : null);
    }

    /**
     * UPDATED filter method that accepts a search field.
     *
     * @param query The text to search for.
     * @param field The field to search within.
     */
    public void filter(String query, SearchField field) {
        if (query == null || query.isEmpty()) {
            super.submitList(new ArrayList<>(originalList));
            return;
        }

        List<OrderEntity> newFilteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (OrderEntity order : originalList) {
            boolean matches = false;
            switch (field) {
                case ID:
                    matches = String.valueOf(order.getOrderId()).contains(lowerCaseQuery);
                    break;
                case DATE:
                    matches = order.getDate().toLowerCase().contains(lowerCaseQuery);
                    break;
                case CUSTOMER:
                    matches = String.valueOf(order.getCustomerId()).contains(lowerCaseQuery);
                    break;
                case USER:
                    matches = String.valueOf(order.getUserId()).contains(lowerCaseQuery);
                    break;
                case ALL:
                default:
                    matches = String.valueOf(order.getOrderId()).contains(lowerCaseQuery) ||
                            order.getDate().toLowerCase().contains(lowerCaseQuery) ||
                            String.valueOf(order.getCustomerId()).contains(lowerCaseQuery) ||
                            String.valueOf(order.getUserId()).contains(lowerCaseQuery);
                    break;
            }
            if (matches) {
                newFilteredList.add(order);
            }
        }
        super.submitList(newFilteredList);
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