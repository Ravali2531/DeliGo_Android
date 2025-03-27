package com.example.deligoandroid.Models;

import java.util.List;

public class OrderItem {
    private String name;
    private int quantity;
    private double price;
    private List<CustomizationOption> customizations;

    public OrderItem() {
        // Default constructor required for Firebase
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<CustomizationOption> getCustomizations() {
        return customizations;
    }

    public void setCustomizations(List<CustomizationOption> customizations) {
        this.customizations = customizations;
    }

    // Nested class for customization options
    public static class CustomizationOption {
        private String optionId;
        private String optionName;
        private List<SelectedItem> selectedItems;

        public CustomizationOption() {
            // Default constructor required for Firebase
        }

        public String getOptionId() {
            return optionId;
        }

        public void setOptionId(String optionId) {
            this.optionId = optionId;
        }

        public String getOptionName() {
            return optionName;
        }

        public void setOptionName(String optionName) {
            this.optionName = optionName;
        }

        public List<SelectedItem> getSelectedItems() {
            return selectedItems;
        }

        public void setSelectedItems(List<SelectedItem> selectedItems) {
            this.selectedItems = selectedItems;
        }
    }

    // Nested class for selected items within a customization
    public static class SelectedItem {
        private String id;
        private String name;
        private double price;

        public SelectedItem() {
            // Default constructor required for Firebase
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }
} 