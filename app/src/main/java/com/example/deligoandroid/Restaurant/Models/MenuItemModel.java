package com.example.deligoandroid.Restaurant.Models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class MenuItemModel implements Serializable {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageURL;
    private String category;
    private boolean isAvailable;
    private List<CustomizationOption> customizationOptions;

    public MenuItemModel() {
        // Required empty constructor for Firebase
        this.customizationOptions = new ArrayList<>();
    }

    public MenuItemModel(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.isAvailable = true; // Default to available
        this.customizationOptions = new ArrayList<>(); // Initialize empty list
    }

    public MenuItemModel(String id, String name, String description, double price, String imageURL, 
                   String category, boolean isAvailable, List<CustomizationOption> customizationOptions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
        this.category = category;
        this.isAvailable = isAvailable;
        this.customizationOptions = customizationOptions != null ? customizationOptions : new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    
    public List<CustomizationOption> getCustomizationOptions() { return customizationOptions; }
    public void setCustomizationOptions(List<CustomizationOption> customizationOptions) { 
        this.customizationOptions = customizationOptions != null ? customizationOptions : new ArrayList<>(); 
    }
} 