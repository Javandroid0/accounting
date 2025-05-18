package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.data.repository.UserRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for managing user data
 */
public class UserViewModel extends AndroidViewModel {
    private static final String TAG = "UserViewModel";

    private final UserRepository userRepository;
    private final MutableLiveData<UserEntity> selectedUser = new MutableLiveData<>();
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    /**
     * Get all users from the repository
     */
    public LiveData<List<UserEntity>> getAllUsers() {
        return userRepository.getAllUsers();
    }

    /**
     * Get user by ID
     */
    public LiveData<UserEntity> getUserById(long userId) {
        return userRepository.getUserById(userId);
    }

    /**
     * Set the selected user for editing/details viewing
     */
    public void setSelectedUser(UserEntity user) {
        if (user != null) {
            Log.d(TAG, "Setting selected user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            selectedUser.setValue(user);
        } else {
            Log.w(TAG, "Attempted to set null selected user");
            selectedUser.setValue(null);
        }
    }

    /**
     * Get the currently selected user
     */
    public LiveData<UserEntity> getSelectedUser() {
        return selectedUser;
    }

    /**
     * Get the current logged-in user
     */
    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

    /**
     * Set the current logged-in user
     */
    public void setCurrentUser(UserEntity user) {
        if (user != null) {
            Log.d(TAG, "Setting current user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            currentUser.setValue(user);
        } else {
            Log.w(TAG, "Attempted to set null current user");
            currentUser.setValue(null);
        }
    }

    /**
     * Insert a new user into the database
     */
    public void insertUser(UserEntity user) {
        if (user == null) {
            Log.e(TAG, "Cannot insert null user");
            return;
        }

        Log.d(TAG, "Inserting user: " + user.getUsername());
        executor.execute(() -> {
            userRepository.insert(user);
        });
    }

    /**
     * Update an existing user in the database
     */
    public void updateUser(UserEntity user) {
        if (user == null) {
            Log.e(TAG, "Cannot update null user");
            return;
        }

        Log.d(TAG, "Updating user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
        executor.execute(() -> {
            userRepository.update(user);
        });
    }

    /**
     * Delete a user from the database
     */
    public void deleteUser(UserEntity user) {
        if (user == null) {
            Log.e(TAG, "Cannot delete null user");
            return;
        }

        Log.d(TAG, "Deleting user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
        executor.execute(() -> {
            userRepository.delete(user);
        });
    }

    /**
     * Get all users synchronously
     */
    public List<UserEntity> getAllUsersSync() {
        try {
            return userRepository.getAllUsersSync();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching users synchronously", e);
            return null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the executor when ViewModel is cleared
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}