package com.example.deligoandroid.Models;

import java.util.Map;

public class CartItem {
    private String id;
    private String menuItemId;
    private String name;
    private String description;
    private double price;
    private String imageURL;
    private int quantity;
    private Map<String, String> customizations;
    private String specialInstructions;
    private double totalPrice;

    public CartItem() {
        // Required empty constructor for Firebase
    }

    public CartItem(String id, String menuItemId, String name, String description, double price,
                   String imageURL, int quantity, Map<String, String> customizations,
                   String specialInstructions) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
        this.quantity = quantity;
        this.customizations = customizations;
        this.specialInstructions = specialInstructions;
        this.totalPrice = price * quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        updateTotalPrice();
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateTotalPrice();
    }

    public Map<String, String> getCustomizations() {
        return customizations;
    }

    public void setCustomizations(Map<String, String> customizations) {
        this.customizations = customizations;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    private void updateTotalPrice() {
        this.totalPrice = this.price * this.quantity;
    }
} 