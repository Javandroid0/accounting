package com.javandroid.accounting_app.ui.fragment.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.viewmodel.user.UserViewModel;

public class UserEditFragment extends Fragment {
    private UserViewModel userViewModel;
    private EditText etUsername, etPassword;
    private Button btnSaveUser;
    private UserEntity currentUser;
    private boolean isEditing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnSaveUser = view.findViewById(R.id.btnSaveUser);

        userViewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                populateFields(user);
                isEditing = true;
            }
        });

        btnSaveUser.setOnClickListener(v -> saveUser());
    }

    private void populateFields(UserEntity user) {
        etUsername.setText(user.getUsername());
        // Don't populate password for security reasons
        etPassword.setHint("Enter new password (leave blank to keep current)");
    }

    private void saveUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            return;
        }

        if (isEditing) {
            // Update existing user
            UserEntity updatedUser = new UserEntity(
                    username,
                    password.isEmpty() ? currentUser.getPassword() : password);
            updatedUser.setUserId(currentUser.getUserId());
            userViewModel.updateUser(updatedUser);
            Toast.makeText(requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Create new user
            if (password.isEmpty()) {
                etPassword.setError("Password is required for new user");
                return;
            }

            UserEntity newUser = new UserEntity(username, password);
            userViewModel.insertUser(newUser);
            Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_SHORT).show();
        }

        // Navigate back
        requireActivity().onBackPressed();
    }
}