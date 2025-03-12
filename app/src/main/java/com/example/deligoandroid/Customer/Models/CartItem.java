package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CartItem implements Serializable {
    private String id;
    private String menuItemId;
    private String name;
    private String description;
    private double price;
    private int quantity;
    private String imageURL;
    private String restaurantId;
    private Map<String, List<CustomizationSelection>> customizations;
    private double totalPrice;
    private double timestamp;

    public CartItem() {
        this.customizations = new HashMap<>();
        this.quantity = 1;
        this.id = "";
        this.menuItemId = "";
        this.name = "";
        this.description = "";
        this.price = 0.0;
        this.imageURL = "";
        this.restaurantId = "";
        this.totalPrice = 0.0;
        this.timestamp = System.currentTimeMillis();
    }

    public CartItem(MenuItem menuItem) {
        this.id = menuItem.getId();
        this.menuItemId = menuItem.getId();
        this.name = menuItem.getName();
        this.description = menuItem.getDescription();
        this.price = menuItem.getPrice();
        this.quantity = 1;
        this.imageURL = menuItem.getImageURL();
        this.customizations = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.totalPrice = menuItem.getPrice();
        this.restaurantId = ""; // Will be set when adding to cart
    }

    // Getters and Setters with null checks
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getMenuItemId() {
        return menuItemId != null ? menuItemId : "";
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId != null ? menuItemId : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public String getImageURL() {
        return imageURL != null ? imageURL : "";
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL != null ? imageURL : "";
    }

    public String getRestaurantId() {
        return restaurantId != null ? restaurantId : "";
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId != null ? restaurantId : "";
    }

    public Map<String, List<CustomizationSelection>> getCustomizations() {
        return customizations != null ? customizations : new HashMap<>();
    }

    public void setCustomizations(Map<String, List<CustomizationSelection>> customizations) {
        this.customizations = customizations != null ? customizations : new HashMap<>();
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    private void updateTotalPrice() {
        double total = price * quantity;
        if (customizations != null) {
            for (List<CustomizationSelection> selections : customizations.values()) {
                for (CustomizationSelection selection : selections) {
                    if (selection.getSelectedItems() != null) {
                        for (SelectedItem item : selection.getSelectedItems()) {
                            total += item.getPrice() * quantity;
                        }
                    }
                }
            }
        }
        this.totalPrice = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CartItem cartItem = (CartItem) o;

        if (Double.compare(cartItem.price, price) != 0) return false;
        if (quantity != cartItem.quantity) return false;
        if (Double.compare(cartItem.totalPrice, totalPrice) != 0) return false;
        if (!id.equals(cartItem.id)) return false;
        if (!menuItemId.equals(cartItem.menuItemId)) return false;
        if (!name.equals(cartItem.name)) return false;
        if (!description.equals(cartItem.description)) return false;
        if (imageURL != null ? !imageURL.equals(cartItem.imageURL) : cartItem.imageURL != null)
            return false;
        return customizations != null ? customizations.equals(cartItem.customizations) : cartItem.customizations == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id.hashCode();
        result = 31 * result + menuItemId.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        temp = Double.doubleToLongBits(price);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + quantity;
        result = 31 * result + (imageURL != null ? imageURL.hashCode() : 0);
        result = 31 * result + (customizations != null ? customizations.hashCode() : 0);
        temp = Double.doubleToLongBits(totalPrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
} 