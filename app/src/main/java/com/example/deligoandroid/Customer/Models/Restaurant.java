package com.example.deligoandroid.Customer.Models;

public class Restaurant {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String address;
    private double rating;
    private boolean isOpen;
    private String cuisineType;

    public Restaurant() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
} 