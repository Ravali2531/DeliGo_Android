package com.example.deligoandroid.Restaurant.Models;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class OrderItem {
    private String menuItemId;
    private String name;
    private String description;
    private int quantity;
    private double price;
    private String specialInstructions;
    private List<Customization> customizations;

    public OrderItem() {
        // Initialize with default values
        this.quantity = 1;
        this.price = 0.0;
        this.specialInstructions = "";
        this.customizations = new ArrayList<>();
    }

    @PropertyName("menuItemId")
    public String getMenuItemId() { return menuItemId != null ? menuItemId : ""; }
    @PropertyName("menuItemId")
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }

    @PropertyName("name")
    public String getName() { return name != null ? name : ""; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("description")
    public String getDescription() { return description != null ? description : ""; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("quantity")
    public int getQuantity() { return Math.max(1, quantity); }
    @PropertyName("quantity")
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }

    @PropertyName("price")
    public double getPrice() { return Math.max(0, price); }
    @PropertyName("price")
    public void setPrice(double price) { this.price = price; }

    @PropertyName("specialInstructions")
    public String getSpecialInstructions() { return specialInstructions != null ? specialInstructions : ""; }
    @PropertyName("specialInstructions")
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    @PropertyName("customizations")
    public List<Customization> getCustomizations() { return customizations != null ? customizations : new ArrayList<>(); }
    @PropertyName("customizations")
    public void setCustomizations(List<Customization> customizations) { this.customizations = customizations; }

    public static class Customization {
        private String choice;
        private String name;
        private double price;

        public Customization() {}

        @PropertyName("choice")
        public String getChoice() { return choice != null ? choice : ""; }
        @PropertyName("choice")
        public void setChoice(String choice) { this.choice = choice; }

        @PropertyName("name")
        public String getName() { return name != null ? name : ""; }
        @PropertyName("name")
        public void setName(String name) { this.name = name; }

        @PropertyName("price")
        public double getPrice() { return price; }
        @PropertyName("price")
        public void setPrice(double price) { this.price = price; }
    }
} 