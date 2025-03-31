package com.javandroid.accounting_app.model;

// Represents a Product
public class Product {
    private int id;
    private String barcode;
    private String name;
    private double sellPrice;
    private double buyPrice;

    public Product(){
    }
    public Product(int id, String barcode, String name, double sellPrice, double buyPrice){
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.sellPrice = sellPrice;
        this.buyPrice = buyPrice;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    // Constructor, Getters, Setters
}