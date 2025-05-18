package com.javandroid.accounting_app.data.model;

/**
 * Data class to hold user profit information
 */
public class UserProfitData {
    private final long userId;
    private final String username;
    private final double totalProfit;
    private final double customerSpecificProfit;

    public UserProfitData(long userId, String username, double totalProfit, double customerSpecificProfit) {
        this.userId = userId;
        this.username = username;
        this.totalProfit = totalProfit;
        this.customerSpecificProfit = customerSpecificProfit;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public double getCustomerSpecificProfit() {
        return customerSpecificProfit;
    }
}