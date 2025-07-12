package com.javandroid.accounting_app.ui.adapter.newOrder;

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

import java.text.NumberFormat;
import java.util.Locale;

public class OrderListAdapter extends ListAdapter<OrderEntity, OrderListAdapter.OrderViewHolder> {

    private final OrderClickListener clickListener;

    public interface OrderClickListener {
        void onOrderClick(OrderEntity order);
    }

    private static final DiffUtil.ItemCallback<OrderEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<OrderEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.getOrderId() == newItem.getOrderId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.getDate().equals(newItem.getDate()) &&
                    oldItem.getTotal() == newItem.getTotal();
        }
    };

    public OrderListAdapter(OrderClickListener listener) {
        super(DIFF_CALLBACK);
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderEntity order = getItem(position);
        holder.bind(order, clickListener);
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderId;
        private final TextView tvOrderDate;
        private final TextView tvOrderTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
        }

        void bind(OrderEntity order, OrderClickListener listener) {
            tvOrderId.setText("Order #" + order.getOrderId());
            tvOrderDate.setText(order.getDate());

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            tvOrderTotal.setText(currencyFormat.format(order.getTotal()));

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }
    }
}