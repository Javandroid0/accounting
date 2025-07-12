package com.javandroid.accounting_app.ui.fragment.user;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.adapter.user.UserListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.user.UserViewModel;

import java.util.List;

public class UserSelectionDialogFragment extends DialogFragment implements UserListAdapter.UserClickListener {

    private static final String TAG = "UserSelectionDialog";
    public static final String ARG_MODE = "mode";
    public static final String MODE_DELETE = "delete";
    public static final String MODE_EDIT = "edit";

    private UserViewModel userViewModel;
    private UserListAdapter adapter;
    private String mode;

    public static UserSelectionDialogFragment newInstance(String mode) {
        UserSelectionDialogFragment fragment = new UserSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE, MODE_EDIT);
        }
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        Log.d(TAG, "DialogFragment created with mode: " + mode);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_user_selection, null);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new UserListAdapter(this);
        recyclerView.setAdapter(adapter);

        // Prefetch users so we have them when the dialog shows
        List<UserEntity> users = userViewModel.getAllUsersSync();
        if (users != null) {
            Log.d(TAG, "Pre-loaded " + users.size() + " users");
            adapter.submitList(users);
        } else {
            Log.d(TAG, "No users loaded synchronously");
        }

        String title = mode.equals(MODE_DELETE) ? "Select User to Delete" : "Select User to Edit";
        builder.setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", (dialog, id) -> dismiss());

        return builder.create();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe users from ViewModel
        userViewModel.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                Log.d(TAG, "Observed " + users.size() + " users");
                adapter.submitList(users);
            } else {
                Log.d(TAG, "Observed null users list");
            }
        });
    }

    @Override
    public void onUserClick(UserEntity user) {
        if (user == null) {
            Log.e(TAG, "Clicked on null user");
            return;
        }

        Log.d(TAG, "User clicked: " + user.getUsername() + " (ID: " + user.getUserId() + ")");

        if (mode.equals(MODE_DELETE)) {
            showDeleteConfirmation(user);
        } else {
            // Set the user for editing
            userViewModel.setSelectedUser(user);
            // Navigate to user edit screen
            dismiss();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new UserEditFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showDeleteConfirmation(UserEntity user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete user " + user.getUsername() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    userViewModel.deleteUser(user);
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}