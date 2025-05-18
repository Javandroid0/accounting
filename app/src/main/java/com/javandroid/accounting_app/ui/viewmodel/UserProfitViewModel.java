package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.data.model.UserProfitData;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfitViewModel extends AndroidViewModel {
    private static final String TAG = "UserProfitViewModel";

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MutableLiveData<List<UserProfitData>> userProfitList = new MutableLiveData<>();
    private final MutableLiveData<Double> totalProfitAcrossAllUsers = new MutableLiveData<>(0.0);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UserProfitViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        orderRepository = new OrderRepository(application);
    }

    public void loadAllUserProfits() {
        executor.execute(() -> {
            try {
                List<UserEntity> users = userRepository.getAllUsersSync();
                List<UserProfitData> profitDataList = new ArrayList<>();
                double totalProfit = 0.0;

                if (users != null) {
                    for (UserEntity user : users) {
                        // Calculate profit for this user
                        double userProfit = orderRepository.calculateProfitByUserSync(user.getUserId());

                        // Create UserProfitData object
                        UserProfitData profitData = new UserProfitData(
                                user.getUserId(),
                                user.getUsername(),
                                userProfit,
                                0.0 // No customer-specific profit by default
                        );

                        profitDataList.add(profitData);
                        totalProfit += userProfit;
                    }

                    // Post values to LiveData
                    userProfitList.postValue(profitDataList);
                    totalProfitAcrossAllUsers.postValue(totalProfit);

                    Log.d(TAG, "Loaded profit data for " + users.size() + " users. Total profit: " + totalProfit);
                } else {
                    Log.e(TAG, "Failed to load users for profit calculation");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading user profits: " + e.getMessage(), e);
            }
        });
    }

    public LiveData<List<UserProfitData>> getUserProfitList() {
        return userProfitList;
    }

    public LiveData<Double> getTotalProfitAcrossAllUsers() {
        return totalProfitAcrossAllUsers;
    }

    public void calculateProfitForUser(long userId) {
        executor.execute(() -> {
            try {
                UserEntity user = userRepository.getUserByIdSync(userId);
                if (user != null) {
                    double userProfit = orderRepository.calculateProfitByUserSync(userId);

                    // Update the list with new profit value
                    List<UserProfitData> currentList = userProfitList.getValue();
                    if (currentList != null) {
                        List<UserProfitData> updatedList = new ArrayList<>();
                        for (UserProfitData data : currentList) {
                            if (data.getUserId() == userId) {
                                updatedList.add(new UserProfitData(
                                        userId, user.getUsername(), userProfit, data.getCustomerSpecificProfit()));
                            } else {
                                updatedList.add(data);
                            }
                        }
                        userProfitList.postValue(updatedList);

                        // Recalculate total profit
                        double total = 0;
                        for (UserProfitData data : updatedList) {
                            total += data.getTotalProfit();
                        }
                        totalProfitAcrossAllUsers.postValue(total);
                    }

                    Log.d(TAG, "Updated profit for user " + userId + ": " + userProfit);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating profit for user " + userId + ": " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}