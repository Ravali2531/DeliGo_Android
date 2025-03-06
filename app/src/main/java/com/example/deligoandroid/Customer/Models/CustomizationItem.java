package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;

public class CustomizationItem implements Serializable {
    private String id;
    private String name;
    private double price;

    public CustomizationItem() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
} 