package com.javandroid.accounting_app.ui.adapter.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.data.model.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class UserDrawerAdapter extends RecyclerView.Adapter<UserDrawerAdapter.UserViewHolder> {

    private List<UserEntity> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserDrawerAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity currentUser = users.get(position);
        holder.textView.setText(currentUser.getUsername());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(currentUser);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<UserEntity> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        UserViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }

    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }
}