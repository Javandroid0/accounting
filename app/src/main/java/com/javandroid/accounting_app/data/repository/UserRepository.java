package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UserRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void insert(User user) {
        executor.execute(() -> db.userDao().insert(user));
    }

    public LiveData<List<User>> getAllUsers() {
        return db.userDao().getAllUsers();
    }

    public void update(User user) {
        executor.execute(() -> db.userDao().update(user));
    }

    public void delete(User user) {
        executor.execute(() -> db.userDao().delete(user));
    }

    public User getUserByIdSync(long id) {
        return db.userDao().getUserById(id); // background thread only!
    }
}
