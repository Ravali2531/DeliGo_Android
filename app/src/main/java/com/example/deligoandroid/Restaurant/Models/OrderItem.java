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
    private List<CustomizationOption> customizations;

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
    public List<CustomizationOption> getCustomizations() { return customizations != null ? customizations : new ArrayList<>(); }
    @PropertyName("customizations")
    public void setCustomizations(List<CustomizationOption> customizations) { this.customizations = customizations; }

    public static class CustomizationOption {
        private String optionId;
        private String optionName;
        private List<SelectedItem> selectedItems;

        public CustomizationOption() {
            this.selectedItems = new ArrayList<>();
        }

        @PropertyName("optionId")
        public String getOptionId() { return optionId; }
        @PropertyName("optionId")
        public void setOptionId(String optionId) { this.optionId = optionId; }

        @PropertyName("optionName")
        public String getOptionName() { return optionName; }
        @PropertyName("optionName")
        public void setOptionName(String optionName) { this.optionName = optionName; }

        @PropertyName("selectedItems")
        public List<SelectedItem> getSelectedItems() { return selectedItems; }
        @PropertyName("selectedItems")
        public void setSelectedItems(List<SelectedItem> selectedItems) { this.selectedItems = selectedItems; }
    }

    public static class SelectedItem {
        private String id;
        private String name;
        private double price;

        public SelectedItem() {}

        @PropertyName("id")
        public String getId() { return id; }
        @PropertyName("id")
        public void setId(String id) { this.id = id; }

        @PropertyName("name")
        public String getName() { return name; }
        @PropertyName("name")
        public void setName(String name) { this.name = name; }

        @PropertyName("price")
        public double getPrice() { return price; }
        @PropertyName("price")
        public void setPrice(double price) { this.price = price; }
    }
} 