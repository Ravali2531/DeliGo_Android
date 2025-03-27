package com.example.deligoandroid.Models;

public class CustomizationSelectedItem {
    private String id;
    private String name;
    private double price;

    public CustomizationSelectedItem() {
        // Required empty constructor for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
} 