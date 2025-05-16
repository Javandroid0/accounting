package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.data.repository.UserRepository;

import java.util.List;

public class UserViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();
    private final LiveData<List<UserEntity>> allUsers;

    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        allUsers = userRepository.getAllUsers();
    }

    public void login(String username, String password) {
        userRepository.getUserByCredentials(username, password, user -> {
            if (user != null) {
                currentUser.setValue(user);
            }
        });
    }

    public void logout() {
        currentUser.setValue(null);
    }

    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return allUsers;
    }

    public void insert(UserEntity user) {
        userRepository.insert(user);
    }

    public void update(UserEntity user) {
        userRepository.update(user);
    }

    public void delete(UserEntity user) {
        userRepository.delete(user);
    }

    public boolean isLoggedIn() {
        return currentUser.getValue() != null;
    }

    public long getCurrentUserId() {
        UserEntity user = currentUser.getValue();
        return user != null ? user.getUserId() : -1;
    }

    public LiveData<UserEntity> getUserById(long userId) {
        return userRepository.getUserById(userId);
    }

    public LiveData<UserEntity> authenticateUser(String username, String password) {
        return userRepository.authenticateUser(username, password);
    }

    public void setCurrentUser(UserEntity user) {
        currentUser.setValue(user);
    }
}