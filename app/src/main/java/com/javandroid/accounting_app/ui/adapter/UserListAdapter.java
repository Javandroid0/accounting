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
import com.javandroid.accounting_app.data.model.UserEntity;

public class UserListAdapter extends ListAdapter<UserEntity, UserListAdapter.UserViewHolder> {
    private final UserClickListener listener;

    public UserListAdapter(UserClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<UserEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return oldItem.getUserId() == newItem.getUserId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return oldItem.getUsername().equals(newItem.getUsername());
        }
    };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = getItem(position);
        holder.bind(user, listener);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }

        public void bind(final UserEntity user, final UserClickListener listener) {
            tvUsername.setText(user.getUsername());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }

    public interface UserClickListener {
        void onUserClick(UserEntity user);
    }
}