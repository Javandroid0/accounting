package com.javandroid.accounting_app.ui.fragment.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.viewmodel.user.UserViewModel;

public class AddUserFragment extends Fragment {

    private UserViewModel userViewModel;
    private EditText etUsername, etPassword, etFullName, etRole;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        // etFullName = view.findViewById(R.id.etFullName);
        // etRole = view.findViewById(R.id.etRole);

        MaterialButton btnSave = view.findViewById(R.id.btnSaveUser);
        btnSave.setOnClickListener(v -> saveUser());

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelUser);
        btnCancel.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
    }

    private void saveUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        // String fullName = etFullName.getText().toString().trim();
        // String role = etRole.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Username and password are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a user entity
        UserEntity user = new UserEntity(username, password);
        // Insert the user into the database
        userViewModel.insertUser(user);
        Toast.makeText(requireContext(), "User added successfully", Toast.LENGTH_SHORT).show();

        Navigation.findNavController(requireView()).navigateUp();
    }
}