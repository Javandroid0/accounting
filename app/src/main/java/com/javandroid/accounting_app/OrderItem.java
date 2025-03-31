package com.javandroid.accounting_app;
// Represents an Order Item within an order
public class OrderItem {
    private int productId;
    private String productName;
    private double productSellPrice;
    private double productBuyPrice;
    private int quantity;

    public OrderItem(int productId, String productName, double productSellPrice, double productBuyPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productSellPrice = productSellPrice;
        this.productBuyPrice = productBuyPrice;
        this.quantity = quantity;
    }

    // Constructor, Getters, Setters

    public double getItemTotalCost() {
        return productBuyPrice * quantity;
    }

    public double getItemTotalSellPrice() {
        return productSellPrice * quantity;
    }

    public double getItemProfit() {
        return getItemTotalSellPrice() - getItemTotalCost();
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductSellPrice(double productSellPrice) {
        this.productSellPrice = productSellPrice;
    }

    public void setProductBuyPrice(double productBuyPrice) {
        this.productBuyPrice = productBuyPrice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getProductSellPrice(){
        return this.productSellPrice;
    }

    public double getProductBuyPrice(){
        return this.productSellPrice;
    }
    public int getQuantity(){
        return this.quantity;
    }
}
