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
import com.javandroid.accounting_app.data.model.UserProfitData;

public class UserProfitAdapter extends ListAdapter<UserProfitData, UserProfitAdapter.UserProfitViewHolder> {

    public UserProfitAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<UserProfitData> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserProfitData>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserProfitData oldItem, @NonNull UserProfitData newItem) {
            return oldItem.getUserId() == newItem.getUserId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserProfitData oldItem, @NonNull UserProfitData newItem) {
            return oldItem.getTotalProfit() == newItem.getTotalProfit() &&
                    oldItem.getUsername().equals(newItem.getUsername());
        }
    };

    @NonNull
    @Override
    public UserProfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_profit, parent, false);
        return new UserProfitViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserProfitViewHolder holder, int position) {
        UserProfitData userProfit = getItem(position);
        holder.bind(userProfit);
    }

    static class UserProfitViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername;
        private final TextView tvProfit;

        public UserProfitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvProfit = itemView.findViewById(R.id.tvUserProfit);
        }

        public void bind(UserProfitData userProfit) {
            tvUsername.setText(userProfit.getUsername());
            tvProfit.setText(String.format("₹%.2f", userProfit.getTotalProfit()));
        }
    }
}