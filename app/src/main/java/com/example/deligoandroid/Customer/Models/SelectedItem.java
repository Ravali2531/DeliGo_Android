package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;

public class SelectedItem implements Serializable {
    private String id;
    private String name;
    private double price;

    public SelectedItem() {
        // Required empty constructor for Firebase
    }

    public SelectedItem(CustomizationItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SelectedItem that = (SelectedItem) o;

        if (Double.compare(that.price, price) != 0) return false;
        if (!id.equals(that.id)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id.hashCode();
        result = 31 * result + name.hashCode();
        temp = Double.doubleToLongBits(price);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
} 