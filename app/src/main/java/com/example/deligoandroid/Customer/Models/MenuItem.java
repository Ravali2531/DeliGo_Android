package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class MenuItem implements Serializable {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageURL;
    private String category;
    private boolean isAvailable;
    private boolean hasCustomizations;
    private List<CustomizationOption> customizationOptions;

    public MenuItem() {
        // Required empty constructor for Firebase
        this.id = "";
        this.name = "";
        this.description = "";
        this.price = 0.0;
        this.imageURL = "";
        this.category = "";
        this.isAvailable = true;
        this.hasCustomizations = false;
        this.customizationOptions = new ArrayList<>();
    }

    // Getters and Setters with null checks
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id != null ? id : ""; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageURL() { return imageURL != null ? imageURL : ""; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL != null ? imageURL : ""; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category != null ? category : ""; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean hasCustomizations() { return hasCustomizations; }
    public void setHasCustomizations(boolean hasCustomizations) { this.hasCustomizations = hasCustomizations; }

    public List<CustomizationOption> getCustomizationOptions() { 
        return customizationOptions != null ? customizationOptions : new ArrayList<>(); 
    }
    public void setCustomizationOptions(List<CustomizationOption> customizationOptions) { 
        this.customizationOptions = customizationOptions != null ? customizationOptions : new ArrayList<>(); 
    }
} 