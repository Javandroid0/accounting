package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.dao.UserDao;
import com.javandroid.accounting_app.data.model.UserEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executor;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void getUserByCredentials(String username, String password, OnUserResultCallback callback) {
        executor.execute(() -> {
            UserEntity user = userDao.getUserByCredentials(username, password);
            callback.onResult(user);
        });
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return userDao.getAllUsers();
    }

    public void insert(UserEntity user) {
        executor.execute(() -> userDao.insert(user));
    }

    public void update(UserEntity user) {
        executor.execute(() -> userDao.update(user));
    }

    public void delete(UserEntity user) {
        executor.execute(() -> userDao.delete(user));
    }

    public void deleteAll() {
        executor.execute(userDao::deleteAll);
    }

    public UserEntity getUserByIdSync(long userId) {
        return userDao.getUserByIdSync(userId);
    }

    public LiveData<UserEntity> getUserById(long userId) {
        return userDao.getUserById(userId);
    }

    /**
     * Authenticates a user with username and password
     * 
     * @param username The username to check
     * @param password The password to verify
     * @return LiveData containing the user if authentication succeeds, or null if
     *         it fails
     */
    public LiveData<UserEntity> authenticateUser(String username, String password) {
        MutableLiveData<UserEntity> result = new MutableLiveData<>();
        executor.execute(() -> {
            UserEntity user = userDao.getUserByCredentials(username, password);
            result.postValue(user);
        });
        return result;
    }

    public interface OnUserResultCallback {
        void onResult(UserEntity user);
    }
}
